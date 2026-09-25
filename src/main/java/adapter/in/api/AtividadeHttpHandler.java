package adapter.in.api;

import application.atividade.AtividadeRepository;
import application.evento.EventoRepository;
import application.usuario.UsuarioRepository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import domain.atividade.Atividade;
import domain.atividade.AtividadeInvalidaException;
import domain.atividade.VinculoPessoa;
import domain.evento.Evento;
import domain.usuario.Papel;
import domain.usuario.Usuario;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Contexto "/atividades". Listar aceita filtros combináveis por query string (RF-09): eventoId,
 * data, trilha, tipo, local — todos opcionais.
 */
public class AtividadeHttpHandler implements HttpHandler {

    private static final Papel[] PODE_GERENCIAR_ATIVIDADES = {
        Papel.ORGANIZADOR, Papel.ADMINISTRADOR
    };

    private final AtividadeRepository repository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Autenticador autenticador;

    public AtividadeHttpHandler(
            AtividadeRepository repository,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository,
            Autenticador autenticador) {
        this.repository = repository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.autenticador = autenticador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpJson.tratarPreflight(exchange)) {
            return;
        }

        try {
            rotear(exchange);
        } catch (AtividadeInvalidaException | DateTimeParseException e) {
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
            if (metodo.equals("GET")) {
                listar(exchange);
                return;
            }
            if (metodo.equals("POST")) {
                criar(exchange);
                return;
            }
        } else if (partes.length == 1) {
            Long id = Long.valueOf(partes[0]);
            if (metodo.equals("GET")) {
                buscar(exchange, id);
                return;
            }
            if (metodo.equals("PUT")) {
                editar(exchange, id);
                return;
            }
            if (metodo.equals("DELETE")) {
                remover(exchange, id);
                return;
            }
        } else if (partes.length == 2 && partes[1].equals("pessoas")) {
            Long id = Long.valueOf(partes[0]);
            if (metodo.equals("GET")) {
                listarPessoas(exchange, id);
                return;
            }
            if (metodo.equals("POST")) {
                vincularPessoa(exchange, id);
                return;
            }
        }

        HttpJson.responder(exchange, 404, HttpJson.erro("Rota não encontrada."));
    }

    private void listar(HttpExchange exchange) throws IOException {
        Long eventoId = parametroLong(exchange, "eventoId");
        LocalDate data = parametroData(exchange, "data");
        String trilha = parametroTexto(exchange, "trilha");
        String tipo = parametroTexto(exchange, "tipo");
        String local = parametroTexto(exchange, "local");

        JSONArray json = new JSONArray();
        boolean gestor = autenticador.podeGerenciar(exchange);
        repository.buscar(eventoId, data, trilha, tipo, local).stream()
                .filter(
                        a ->
                                gestor
                                        || a.getEvento().getStatus()
                                                == domain.evento.StatusEvento.PUBLICADO)
                .forEach(atividade -> json.put(paraJson(atividade)));
        HttpJson.responder(exchange, 200, json);
    }

    private void criar(HttpExchange exchange) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Atividade candidata =
                Atividade.nova(
                        corpo.optString("titulo", null),
                        corpo.optString("descricao", null),
                        corpo.optString("tipo", null),
                        corpo.optString("trilha", null),
                        corpo.optString("local", null),
                        data(corpo, "inicio"),
                        data(corpo, "fim"),
                        corpo.has("capacidade") && !corpo.isNull("capacidade")
                                ? corpo.getInt("capacidade")
                                : null,
                        evento(corpo));
        HttpJson.responder(
                exchange,
                201,
                paraJson(
                        new application.atividade.ProgramacaoService(repository)
                                .salvar(candidata)));
    }

    private void buscar(HttpExchange exchange, Long id) throws IOException {
        Optional<Atividade> atividade = repository.buscarPorId(id);
        if (atividade.isEmpty()
                || (!autenticador.podeGerenciar(exchange)
                        && atividade.get().getEvento().getStatus()
                                != domain.evento.StatusEvento.PUBLICADO)) {
            HttpJson.responder(
                    exchange, 404, HttpJson.erro("Atividade " + id + " não encontrada."));
            return;
        }
        HttpJson.responder(exchange, 200, paraJson(atividade.get()));
    }

    private void editar(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        Optional<Atividade> existente = repository.buscarPorId(id);
        if (existente.isEmpty()) {
            HttpJson.responder(
                    exchange, 404, HttpJson.erro("Atividade " + id + " não encontrada."));
            return;
        }
        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Atividade atividade = existente.get();
        atividade.editar(
                corpo.optString("titulo", null),
                corpo.optString("descricao", null),
                corpo.optString("tipo", null),
                corpo.optString("trilha", null),
                corpo.optString("local", null),
                data(corpo, "inicio"),
                data(corpo, "fim"),
                corpo.has("capacidade") && !corpo.isNull("capacidade")
                        ? corpo.getInt("capacidade")
                        : null);
        HttpJson.responder(
                exchange,
                200,
                paraJson(
                        new application.atividade.ProgramacaoService(repository)
                                .salvar(atividade)));
    }

    private void remover(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        if (repository.buscarPorId(id).isEmpty()) {
            HttpJson.responder(
                    exchange, 404, HttpJson.erro("Atividade " + id + " não encontrada."));
            return;
        }
        new application.atividade.ProgramacaoService(repository).remover(id);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void listarPessoas(HttpExchange exchange, Long atividadeId) throws IOException {
        var atividade = repository.buscarPorId(atividadeId);
        if (atividade.isEmpty()
                || (!autenticador.podeGerenciar(exchange)
                        && atividade.get().getEvento().getStatus()
                                != domain.evento.StatusEvento.PUBLICADO)) {
            HttpJson.responder(exchange, 404, HttpJson.erro("Atividade não encontrada."));
            return;
        }
        JSONArray json = new JSONArray();
        repository.listarPessoas(atividadeId).forEach(vinculo -> json.put(paraJson(vinculo)));
        HttpJson.responder(exchange, 200, json);
    }

    private void vincularPessoa(HttpExchange exchange, Long atividadeId) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        if (repository.buscarPorId(atividadeId).isEmpty()) {
            HttpJson.responder(
                    exchange, 404, HttpJson.erro("Atividade " + atividadeId + " não encontrada."));
            return;
        }
        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Long usuarioId = corpo.has("usuarioId") ? corpo.getLong("usuarioId") : null;
        String nome =
                usuarioId == null
                        ? null
                        : usuarioRepository
                                .buscarPorId(usuarioId)
                                .map(Usuario::getNome)
                                .orElseThrow(
                                        () ->
                                                new AtividadeInvalidaException(
                                                        "Usuário "
                                                                + usuarioId
                                                                + " não encontrado."));
        VinculoPessoa vinculo = new VinculoPessoa(usuarioId, nome, corpo.optString("papel", null));

        repository.vincularPessoa(atividadeId, vinculo);
        HttpJson.responder(exchange, 201, paraJson(vinculo));
    }

    private Evento evento(JSONObject corpo) {
        if (!corpo.has("eventoId")) {
            throw new AtividadeInvalidaException("eventoId é obrigatório.");
        }
        long eventoId = corpo.getLong("eventoId");
        return eventoRepository
                .buscarPorId(eventoId)
                .orElseThrow(
                        () ->
                                new AtividadeInvalidaException(
                                        "Evento " + eventoId + " não encontrado."));
    }

    private Long parametroLong(HttpExchange exchange, String nome) {
        String valor = parametroTexto(exchange, nome);
        return valor == null ? null : Long.valueOf(valor);
    }

    private LocalDate parametroData(HttpExchange exchange, String nome) {
        String valor = parametroTexto(exchange, nome);
        return valor == null ? null : LocalDate.parse(valor);
    }

    private String parametroTexto(HttpExchange exchange, String nome) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String par : query.split("&")) {
            String[] chaveValor = par.split("=", 2);
            if (chaveValor.length == 2 && chaveValor[0].equals(nome)) {
                return decodificar(chaveValor[1]);
            }
        }
        return null;
    }

    private String decodificar(String valor) {
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return valor;
        }
    }

    private LocalDateTime data(JSONObject corpo, String campo) {
        String valor = corpo.optString(campo, null);
        return valor == null ? null : LocalDateTime.parse(valor);
    }

    private JSONObject paraJson(Atividade atividade) {
        JSONObject json = new JSONObject();
        json.put("id", atividade.getId());
        json.put("titulo", atividade.getTitulo());
        json.put("descricao", atividade.getDescricao());
        json.put("tipo", atividade.getTipo());
        json.put("trilha", atividade.getTrilha());
        json.put("local", atividade.getLocal());
        json.put("inicio", atividade.getInicio().toString());
        json.put("fim", atividade.getFim().toString());
        json.put("capacidade", atividade.getCapacidade());
        json.put("eventoId", atividade.getEvento().getId());
        json.put("fuso", atividade.getEvento().getFuso().getId());
        return json;
    }

    private JSONObject paraJson(VinculoPessoa vinculo) {
        JSONObject json = new JSONObject();
        json.put("usuarioId", vinculo.getUsuarioId());
        json.put("nomePessoa", vinculo.getNomePessoa());
        json.put("papel", vinculo.getPapel());
        return json;
    }

    private String[] caminho(HttpExchange exchange) {
        String resto = exchange.getRequestURI().getPath().replaceFirst("^/atividades/?", "");
        return resto.isBlank() ? new String[0] : resto.split("/");
    }
}
