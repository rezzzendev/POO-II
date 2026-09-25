package adapter.in.api;

import application.atividade.AtividadeRepository;
import application.inscricao.InscricaoRepository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import domain.atividade.Atividade;
import domain.inscricao.Inscricao;
import domain.inscricao.StatusInscricao;
import domain.usuario.Usuario;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RF-16/17/18: agenda pessoal — atividades escolhidas nas inscrições ativas do usuário autenticado,
 * em ordem cronológica.
 */
public class AgendaHttpHandler implements HttpHandler {

    private final InscricaoRepository inscricaoRepository;
    private final AtividadeRepository atividadeRepository;
    private final Autenticador autenticador;

    public AgendaHttpHandler(
            InscricaoRepository inscricaoRepository,
            AtividadeRepository atividadeRepository,
            Autenticador autenticador) {
        this.inscricaoRepository = inscricaoRepository;
        this.atividadeRepository = atividadeRepository;
        this.autenticador = autenticador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpJson.tratarPreflight(exchange)) {
            return;
        }

        try {
            if (!exchange.getRequestMethod().equals("GET")
                    || !exchange.getRequestURI().getPath().equals("/agenda")) {
                HttpJson.responder(exchange, 404, HttpJson.erro("Rota não encontrada."));
                return;
            }
            listar(exchange);
        } catch (NaoAutenticadoException e) {
            HttpJson.responder(exchange, 401, HttpJson.erro(e.getMessage()));
        } catch (IllegalArgumentException
                | org.json.JSONException
                | java.time.DateTimeException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro(e.getMessage()));
        } catch (Exception e) {
            HttpJson.falhaInterna(exchange, e);
        }
    }

    private void listar(HttpExchange exchange) throws IOException {
        Usuario usuario = autenticador.exigir(exchange);

        List<Atividade> agenda = new ArrayList<>();
        for (Inscricao inscricao : inscricaoRepository.listarPorUsuario(usuario.getId())) {
            if (inscricao.getStatus() != StatusInscricao.CONFIRMADA) {
                continue;
            }
            for (Long atividadeId : inscricao.getAtividadeIds()) {
                atividadeRepository.buscarPorId(atividadeId).ifPresent(agenda::add);
            }
        }
        agenda.sort(
                Comparator.comparing(
                        a -> a.getInicio().atZone(a.getEvento().getFuso()).toInstant()));

        JSONArray json = new JSONArray();
        agenda.forEach(atividade -> json.put(paraJson(atividade)));
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
