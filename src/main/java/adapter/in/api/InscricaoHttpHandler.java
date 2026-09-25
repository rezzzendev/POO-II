package adapter.in.api;

import application.atividade.AtividadeRepository;
import application.evento.EventoRepository;
import application.inscricao.InscricaoRepository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import domain.evento.Evento;
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
public class InscricaoHttpHandler implements HttpHandler {

    private static final Papel[] PODE_VER_INSCRITOS = {Papel.ORGANIZADOR, Papel.ADMINISTRADOR};

    private final application.inscricao.InscricaoService service;
    private final InscricaoRepository repository;
    private final EventoRepository eventoRepository;
    private final AtividadeRepository atividadeRepository;
    private final Autenticador autenticador;

    public InscricaoHttpHandler(
            InscricaoRepository repository,
            EventoRepository eventoRepository,
            AtividadeRepository atividadeRepository,
            Autenticador autenticador,
            application.inscricao.InscricaoService service) {
        this.service = service;
        this.repository = repository;
        this.eventoRepository = eventoRepository;
        this.atividadeRepository = atividadeRepository;
        this.autenticador = autenticador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpJson.tratarPreflight(exchange)) {
            return;
        }

        try {
            rotear(exchange);
        } catch (InscricaoInvalidaException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro(e.getMessage()));
        } catch (NumberFormatException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro("Id precisa ser um número."));
        } catch (NaoAutenticadoException e) {
            HttpJson.responder(exchange, 401, HttpJson.erro(e.getMessage()));
        } catch (NaoAutorizadoException e) {
            HttpJson.responder(exchange, 403, HttpJson.erro(e.getMessage()));
        } catch (IllegalArgumentException
                | org.json.JSONException
                | java.time.DateTimeException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro(e.getMessage()));
        } catch (Exception e) {
            HttpJson.falhaInterna(exchange, e);
        }
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
        long eventoId = corpo.getLong("eventoId");
        Evento evento =
                eventoRepository
                        .buscarPorId(eventoId)
                        .orElseThrow(
                                () ->
                                        new InscricaoInvalidaException(
                                                "Evento " + eventoId + " não encontrado."));

        HttpJson.responder(
                exchange,
                201,
                paraJson(service.inscrever(usuario, eventoId, lerAtividadeIds(corpo))));
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
        repository
                .listarPorUsuario(usuario.getId())
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
        repository
                .listarPorEvento(Long.valueOf(eventoIdTexto))
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

    private String parametro(HttpExchange exchange, String nome) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String par : query.split("&")) {
            String[] chaveValor = par.split("=", 2);
            if (chaveValor.length == 2 && chaveValor[0].equals(nome)) {
                return chaveValor[1];
            }
        }
        return null;
    }

    private String[] caminho(HttpExchange exchange) {
        String resto = exchange.getRequestURI().getPath().replaceFirst("^/inscricoes/?", "");
        return resto.isBlank() ? new String[0] : resto.split("/");
    }
}
