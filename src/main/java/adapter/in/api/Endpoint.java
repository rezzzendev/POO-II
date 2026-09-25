package adapter.in.api;

import com.sun.net.httpserver.*;

import org.json.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Template Method: transporte e tratamento de erro comuns, rotas definidas nos handlers. */
public abstract class Endpoint implements HttpHandler {
    public final void handle(HttpExchange e) throws IOException {
        if (HttpJson.tratarPreflight(e)) return;
        try {
            executar(e);
        } catch (NaoAutenticadoException x) {
            HttpJson.responder(e, 401, HttpJson.erro(x.getMessage()));
        } catch (NaoAutorizadoException x) {
            HttpJson.responder(e, 403, HttpJson.erro(x.getMessage()));
        } catch (IllegalArgumentException | JSONException | java.time.DateTimeException x) {
            HttpJson.responder(e, 400, HttpJson.erro(x.getMessage()));
        } catch (Exception x) {
            HttpJson.falhaInterna(e, x);
        }
    }

    protected abstract void executar(HttpExchange e) throws IOException;

    protected String[] partes(HttpExchange e) {
        return e.getRequestURI().getPath().substring(1).split("/");
    }

    protected String parametro(HttpExchange e, String nome) {
        String q = e.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String par : q.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv[0].equals(nome) && kv.length == 2)
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }

    protected void naoEncontrado(HttpExchange e) throws IOException {
        HttpJson.responder(e, 404, HttpJson.erro("Rota não encontrada."));
    }
}
