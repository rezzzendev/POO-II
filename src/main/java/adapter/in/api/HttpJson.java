package adapter.in.api;

import com.sun.net.httpserver.HttpExchange;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Repetido por todo handler de API: ler corpo JSON, responder JSON, CORS. Extraído aqui pra não
 * duplicar entre EventoHttpHandler e UsuarioHttpHandler.
 */
class HttpJson {

    static JSONObject lerCorpo(HttpExchange exchange) throws IOException {
        try (InputStream entrada = exchange.getRequestBody()) {
            byte[] bytes = entrada.readNBytes(2_000_001);
            if (bytes.length > 2_000_000)
                throw new IllegalArgumentException("Corpo da requisição muito grande.");
            String texto = new String(bytes, StandardCharsets.UTF_8);
            return texto.isBlank() ? new JSONObject() : new JSONObject(texto);
        }
    }

    static void responder(HttpExchange exchange, int status, Object corpo) throws IOException {
        byte[] bytes = corpo.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }

    static void falhaInterna(HttpExchange e, Exception erro) throws IOException {
        System.err.println("Falha na API: " + erro.getClass().getSimpleName());
        responder(e, 500, erro("Erro interno. Tente novamente ou contate a organização."));
    }

    static JSONObject erro(String mensagem) {
        return new JSONObject().put("erro", mensagem);
    }

    /**
     * @return true se já respondeu (preflight) e o handler deve parar por aqui.
     */
    static boolean tratarPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (!exchange.getRequestMethod().equals("OPTIONS")) {
            return false;
        }
        exchange.getResponseHeaders()
                .add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders()
                .add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
        return true;
    }
}
