package adapter.in.api;

import application.inscricao.InscricaoService;

import com.sun.net.httpserver.HttpExchange;

import domain.atividade.Atividade;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

/**
 * RF-16/17/18: agenda pessoal — atividades escolhidas nas inscrições ativas do usuário autenticado,
 * em ordem cronológica.
 */
public class AgendaHttpHandler extends Endpoint {

    private final InscricaoService service;
    private final Autenticador autenticador;

    public AgendaHttpHandler(InscricaoService service, Autenticador autenticador) {
        this.service = service;
        this.autenticador = autenticador;
    }

    @Override
    protected void executar(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")
                || !exchange.getRequestURI().getPath().equals("/agenda")) {
            naoEncontrado(exchange);
            return;
        }
        listar(exchange);
    }

    private void listar(HttpExchange exchange) throws IOException {
        JSONArray json = new JSONArray();
        service.agenda(autenticador.exigir(exchange))
                .forEach(atividade -> json.put(paraJson(atividade)));
        HttpJson.responder(exchange, 200, json);
    }

    private JSONObject paraJson(Atividade atividade) {
        JSONObject json = new JSONObject();
        json.put("id", atividade.getId());
        json.put("titulo", atividade.getTitulo());
        json.put("tipo", atividade.getTipo());
        json.put("local", atividade.getLocal());
        json.put("inicio", atividade.getInicio().toString());
        json.put("fim", atividade.getFim().toString());
        json.put("eventoId", atividade.getEvento().getId());
        json.put("fuso", atividade.getEvento().getFuso().getId());
        return json;
    }
}
