package adapter.in.api;

import application.inscricao.InscricaoService;

import com.sun.net.httpserver.HttpExchange;

import domain.inscricao.RegrasInscricao;
import domain.usuario.Papel;

import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;

public class RegrasHttpHandler extends Endpoint {
    private final InscricaoService service;
    private final Autenticador auth;

    public RegrasHttpHandler(InscricaoService s, Autenticador a) {
        service = s;
        auth = a;
    }

    protected void executar(HttpExchange e) throws IOException {
        String[] p = partes(e);
        if (p.length != 2) {
            naoEncontrado(e);
            return;
        }
        long id = Long.parseLong(p[1]);
        if (e.getRequestMethod().equals("PUT")) {
            auth.exigir(e, Papel.ADMINISTRADOR, Papel.ORGANIZADOR);
            JSONObject b = HttpJson.lerCorpo(e);
            service.configurar(
                    id,
                    new RegrasInscricao(
                            b.getBoolean("escolherAtividades"),
                            b.getBoolean("controlarVagas"),
                            LocalDateTime.parse(b.getString("prazoCancelamento"))));
        } else if (!e.getRequestMethod().equals("GET")) {
            naoEncontrado(e);
            return;
        }
        var r = service.regras(id);
        HttpJson.responder(
                e,
                200,
                new JSONObject()
                        .put("escolherAtividades", r.escolherAtividades())
                        .put("controlarVagas", r.controlarVagas())
                        .put("prazoCancelamento", r.prazoCancelamento().toString()));
    }
}
