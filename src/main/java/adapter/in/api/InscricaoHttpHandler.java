package adapter.in.api;

import application.inscricao.InscricaoService;

import com.sun.net.httpserver.HttpExchange;

import domain.inscricao.Inscricao;
import domain.inscricao.InscricaoInvalidaException;
import domain.usuario.Papel;
import domain.usuario.Usuario;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contexto "/inscricoes". RF-10 a RF-15: inscrição sempre é feita pelo próprio usuário autenticado
 * (não dá pra inscrever outra pessoa).
 */
public class InscricaoHttpHandler extends Endpoint {

    private static final Papel[] PODE_VER_INSCRITOS = {Papel.ORGANIZADOR, Papel.ADMINISTRADOR};

    private final InscricaoService service;
    private final Autenticador autenticador;

    public InscricaoHttpHandler(InscricaoService service, Autenticador autenticador) {
        this.service = service;
        this.autenticador = autenticador;
    }

    @Override
    protected void executar(HttpExchange exchange) throws IOException {
        rotear(exchange);
    }

    private void rotear(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String[] partes = caminho(exchange);

        if (partes.length == 0) {
            if (metodo.equals("POST")) {
                criar(exchange);
                return;
            }
            if (metodo.equals("GET")) {
                listarPorEvento(exchange);
                return;
            }
        } else if (partes.length == 1 && partes[0].equals("minhas")) {
            if (metodo.equals("GET")) {
                minhasInscricoes(exchange);
                return;
            }
        } else if (partes.length == 2 && partes[1].equals("cancelar")) {
            if (metodo.equals("POST")) {
                cancelar(exchange, Long.valueOf(partes[0]));
                return;
            }
        } else if (partes.length == 2 && partes[1].equals("atividades")) {
            if (metodo.equals("PUT")) {
                redefinirAtividades(exchange, Long.valueOf(partes[0]));
                return;
            }
        }

        HttpJson.responder(exchange, 404, HttpJson.erro("Rota não encontrada."));
    }

    private void criar(HttpExchange exchange) throws IOException {
        Usuario usuario = autenticador.exigir(exchange);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        if (!corpo.has("eventoId")) {
            throw new InscricaoInvalidaException("eventoId é obrigatório.");
        }
        HttpJson.responder(
                exchange,
                201,
                paraJson(
                        service.inscrever(
                                usuario, corpo.getLong("eventoId"), lerAtividadeIds(corpo))));
    }

    private void redefinirAtividades(HttpExchange exchange, Long inscricaoId) throws IOException {
        HttpJson.responder(
                exchange,
                200,
                paraJson(
                        service.selecionar(
                                autenticador.exigir(exchange),
                                inscricaoId,
                                lerAtividadeIds(HttpJson.lerCorpo(exchange)))));
    }

    private void minhasInscricoes(HttpExchange exchange) throws IOException {
        Usuario usuario = autenticador.exigir(exchange);
        JSONArray json = new JSONArray();
        service.listarDoUsuario(usuario)
                .forEach(inscricao -> json.put(paraJson(inscricao)));
        HttpJson.responder(exchange, 200, json);
    }

    private void listarPorEvento(HttpExchange exchange) throws IOException {
        autenticador.exigir(exchange, PODE_VER_INSCRITOS);

        String eventoIdTexto = parametro(exchange, "eventoId");
        if (eventoIdTexto == null) {
            HttpJson.responder(
                    exchange, 400, HttpJson.erro("Informe ?eventoId= pra listar os inscritos."));
            return;
        }
        JSONArray json = new JSONArray();
        service.listarDoEvento(Long.parseLong(eventoIdTexto))
                .forEach(inscricao -> json.put(paraJson(inscricao)));
        HttpJson.responder(exchange, 200, json);
    }

    private void cancelar(HttpExchange exchange, Long id) throws IOException {
        Usuario usuario = autenticador.exigir(exchange);

        HttpJson.responder(exchange, 200, paraJson(service.cancelar(usuario, id)));
    }

    private List<Long> lerAtividadeIds(JSONObject corpo) {
        List<Long> atividadeIds = new ArrayList<>();
        if (corpo.has("atividadeIds")) {
            JSONArray array = corpo.getJSONArray("atividadeIds");
            for (int i = 0; i < array.length(); i++) {
                atividadeIds.add(array.getLong(i));
            }
        }
        return atividadeIds;
    }

    private JSONObject paraJson(Inscricao inscricao) {
        JSONObject json = new JSONObject();
        json.put("id", inscricao.getId());
        json.put("usuarioId", inscricao.getUsuario().getId());
        json.put("usuarioNome", inscricao.getUsuario().getNome());
        json.put("eventoId", inscricao.getEvento().getId());
        json.put("atividadeIds", inscricao.getAtividadeIds());
        json.put("status", inscricao.getStatus().name());
        json.put("dataInscricao", inscricao.getDataInscricao().toString());
        return json;
    }

    private String[] caminho(HttpExchange exchange) {
        String resto = exchange.getRequestURI().getPath().replaceFirst("^/inscricoes/?", "");
        return resto.isBlank() ? new String[0] : resto.split("/");
    }
}
