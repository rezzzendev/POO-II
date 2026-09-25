package adapter.in.api;

import adapter.out.qr.QrCode;

import application.frequencia.FrequenciaService;

import com.sun.net.httpserver.HttpExchange;

import domain.frequencia.*;
import domain.usuario.Papel;

import org.json.*;

import java.io.IOException;
import java.util.Base64;

public class FrequenciaHttpHandler extends Endpoint {
    private final FrequenciaService service;
    private final Autenticador auth;
    private final QrCode qr = new QrCode();

    public FrequenciaHttpHandler(FrequenciaService s, Autenticador a) {
        service = s;
        auth = a;
    }

    protected void executar(HttpExchange e) throws IOException {
        String[] p = partes(e);
        String m = e.getRequestMethod();
        if (p.length == 2 && p[1].equals("qr") && m.equals("POST")) {
            long u = auth.exigir(e).getId();
            JSONObject b = HttpJson.lerCorpo(e);
            String token =
                    b.has("imagemBase64")
                            ? qr.ler(Base64.getDecoder().decode(b.getString("imagemBase64")))
                            : b.getString("token");
            service.registrarQr(u, token);
            HttpJson.responder(e, 201, new JSONObject().put("mensagem", "Frequência registrada."));
            return;
        }
        if (p.length < 2) {
            naoEncontrado(e);
            return;
        }
        long a = Long.parseLong(p[1]);
        if (p.length == 3 && p[2].equals("politica")) {
            if (m.equals("GET")) {
                auth.exigir(e);
                HttpJson.responder(e, 200, new JSONObject().put("politica", service.politica(a)));
                return;
            }
            if (m.equals("PUT")) {
                gerente(e);
                service.configurar(a, HttpJson.lerCorpo(e).getString("politica"));
                HttpJson.responder(e, 200, new JSONObject().put("politica", service.politica(a)));
                return;
            }
        }
        if (p.length == 3 && p[2].equals("codigos") && m.equals("POST")) {
            gerente(e);
            CodigoFrequencia c =
                    service.gerar(a, TipoMarcacao.valueOf(HttpJson.lerCorpo(e).getString("tipo")));
            HttpJson.responder(
                    e,
                    201,
                    new JSONObject()
                            .put("token", c.token())
                            .put("validade", c.validade().toString())
                            .put(
                                    "imagemBase64",
                                    Base64.getEncoder().encodeToString(qr.gerar(c.token()))));
            return;
        }
        if (p.length == 3 && p[2].equals("manual") && m.equals("POST")) {
            long responsavel = gerente(e);
            JSONObject b = HttpJson.lerCorpo(e);
            service.manual(
                    a,
                    b.getLong("usuarioId"),
                    responsavel,
                    b.getBoolean("presente"),
                    b.getString("justificativa"));
            HttpJson.responder(
                    e,
                    201,
                    new JSONObject().put("mensagem", "Lançamento manual registrado com autoria."));
            return;
        }
        if (p.length == 2 && m.equals("GET")) {
            var usuario = auth.exigir(e);
            String filtro = parametro(e, "usuarioId");
            long u = filtro == null ? usuario.getId() : Long.parseLong(filtro);
            if (u != usuario.getId()) gerente(e);
            JSONArray registros = new JSONArray();
            service.registros(a, u)
                    .forEach(
                            r ->
                                    registros.put(
                                            new JSONObject()
                                                    .put("id", r.id())
                                                    .put("tipo", r.tipo().name())
                                                    .put("origem", r.origem())
                                                    .put("responsavelId", r.responsavelId())
                                                    .put("instante", r.instante().toString())
                                                    .put("justificativa", r.justificativa())));
            HttpJson.responder(
                    e,
                    200,
                    new JSONObject()
                            .put("atividadeId", a)
                            .put("usuarioId", u)
                            .put("politica", service.politica(a))
                            .put("presente", service.presente(a, u))
                            .put("registros", registros));
            return;
        }
        naoEncontrado(e);
    }

    private long gerente(HttpExchange e) {
        return auth.exigir(e, Papel.ADMINISTRADOR, Papel.ORGANIZADOR).getId();
    }
}
