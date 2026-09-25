package adapter.in.api;

import application.relatorio.RelatorioService;

import com.sun.net.httpserver.HttpExchange;

import domain.usuario.Papel;

import org.json.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RelatorioHttpHandler extends Endpoint {
    private final RelatorioService service;
    private final Autenticador auth;

    public RelatorioHttpHandler(RelatorioService s, Autenticador a) {
        service = s;
        auth = a;
    }

    protected void executar(HttpExchange e) throws IOException {
        auth.exigir(e, Papel.ADMINISTRADOR, Papel.ORGANIZADOR);
        String[] p = partes(e);
        if (!e.getRequestMethod().equals("GET")
                || p.length != 2
                || (!p[1].equals("inscritos") && !p[1].equals("frequencia"))) {
            naoEncontrado(e);
            return;
        }
        long evento = Long.parseLong(parametro(e, "eventoId"));
        String filtro = parametro(e, "atividadeId");
        Long atividade = filtro == null ? null : Long.valueOf(filtro);
        List<RelatorioService.Linha> linhas =
                p[1].equals("inscritos")
                        ? service.inscritos(evento, atividade)
                        : service.frequencia(evento, atividade);
        if ("csv".equals(parametro(e, "formato"))) {
            StringBuilder csv =
                    new StringBuilder(
                            "\ufeffusuarioId;nome;email;inscricao;atividadeId;atividade;presente;marcacoes\r\n");
            for (var l : linhas)
                csv.append(l.usuarioId())
                        .append(';')
                        .append(celula(l.nome()))
                        .append(';')
                        .append(celula(l.email()))
                        .append(';')
                        .append(l.inscricao())
                        .append(';')
                        .append(l.atividadeId() == null ? "" : l.atividadeId())
                        .append(';')
                        .append(celula(l.atividade()))
                        .append(';')
                        .append(l.presente())
                        .append(';')
                        .append(l.marcacoes())
                        .append("\r\n");
            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
            e.getResponseHeaders().set("Content-Disposition", "attachment; filename=relatorio.csv");
            e.sendResponseHeaders(200, bytes.length);
            try (var out = e.getResponseBody()) {
                out.write(bytes);
            }
            return;
        }
        JSONArray dados = new JSONArray();
        for (var l : linhas)
            dados.put(
                    new JSONObject()
                            .put("usuarioId", l.usuarioId())
                            .put("nome", l.nome())
                            .put("email", l.email())
                            .put("inscricao", l.inscricao())
                            .put("atividadeId", l.atividadeId())
                            .put("atividade", l.atividade())
                            .put("presente", l.presente())
                            .put("marcacoes", l.marcacoes()));
        HttpJson.responder(
                e,
                200,
                new JSONObject()
                        .put("total", linhas.size())
                        .put(
                                "confirmados",
                                linhas.stream()
                                        .filter(l -> l.inscricao().equals("CONFIRMADA"))
                                        .count())
                        .put(
                                "presentes",
                                linhas.stream().filter(RelatorioService.Linha::presente).count())
                        .put("linhas", dados));
    }

    private String celula(String s) {
        if (s.stripLeading().matches("^[=+@-].*")) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
