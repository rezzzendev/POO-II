package adapter.in.api;

import application.evento.EventoRepository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import domain.evento.Evento;
import domain.evento.EventoInvalidoException;
import domain.evento.Modalidade;
import domain.usuario.Papel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Um único contexto, "/eventos" — sem framework, quem resolve qual rota é este handler mesmo,
 * olhando pro método HTTP e pro caminho.
 *
 * <p>Leitura (GET) é pública (RF-10: visitante consulta sem login). Escrita exige Organizador ou
 * Administrador — verificado aqui no servidor, não só escondido na tela (RNF-06).
 */
public class EventoHttpHandler implements HttpHandler {

    private static final Papel[] PODE_GERENCIAR_EVENTOS = {Papel.ORGANIZADOR, Papel.ADMINISTRADOR};

    private final application.atividade.AtividadeRepository atividades;
    private final EventoRepository repository;
    private final Autenticador autenticador;

    public EventoHttpHandler(
            EventoRepository repository,
            Autenticador autenticador,
            application.atividade.AtividadeRepository atividades) {
        this.atividades = atividades;
        this.repository = repository;
        this.autenticador = autenticador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpJson.tratarPreflight(exchange)) {
            return;
        }

        try {
            rotear(exchange);
        } catch (EventoInvalidoException | DateTimeParseException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro(e.getMessage()));
        } catch (NumberFormatException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro("Id do evento precisa ser um número."));
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
        } else if (partes.length == 2) {
            Long id = Long.valueOf(partes[0]);
            if (metodo.equals("POST") && partes[1].equals("publicar")) {
                transicionar(exchange, id, Evento::publicar);
                return;
            }
            if (metodo.equals("POST") && partes[1].equals("encerrar")) {
                transicionar(exchange, id, Evento::encerrar);
                return;
            }
        }

        HttpJson.responder(exchange, 404, HttpJson.erro("Rota não encontrada."));
    }

    private void listar(HttpExchange exchange) throws IOException {
        List<Evento> eventos = repository.listarTodos();
        JSONArray json = new JSONArray();
        boolean gestor = autenticador.podeGerenciar(exchange);
        eventos.stream()
                .filter(e -> gestor || e.getStatus() == domain.evento.StatusEvento.PUBLICADO)
                .forEach(evento -> json.put(paraJson(evento)));
        HttpJson.responder(exchange, 200, json);
    }

    private void criar(HttpExchange exchange) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Evento evento =
                Evento.novo(
                        corpo.optString("titulo", null),
                        corpo.optString("descricao", null),
                        data(corpo, "inicio"),
                        data(corpo, "fim"),
                        modalidade(corpo));
        evento.definirLocalEFuso(
                corpo.optString("local", "A definir"),
                corpo.optString("fuso", "America/Sao_Paulo"));
        HttpJson.responder(exchange, 201, paraJson(repository.salvar(evento)));
    }

    private void buscar(HttpExchange exchange, Long id) throws IOException {
        Optional<Evento> evento = repository.buscarPorId(id);
        if (evento.isEmpty()
                || (!autenticador.podeGerenciar(exchange)
                        && evento.get().getStatus() != domain.evento.StatusEvento.PUBLICADO)) {
            HttpJson.responder(exchange, 404, HttpJson.erro("Evento " + id + " não encontrado."));
            return;
        }
        HttpJson.responder(exchange, 200, paraJson(evento.get()));
    }

    private void editar(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);

        Optional<Evento> existente = repository.buscarPorId(id);
        if (existente.isEmpty()) {
            HttpJson.responder(exchange, 404, HttpJson.erro("Evento " + id + " não encontrado."));
            return;
        }
        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Evento evento = existente.get();
        var inicio = data(corpo, "inicio");
        var fim = data(corpo, "fim");
        for (var atividade : atividades.listarPorEvento(id)) {
            if (inicio == null
                    || fim == null
                    || atividade.getInicio().isBefore(inicio)
                    || atividade.getFim().isAfter(fim))
                throw new EventoInvalidoException(
                        "O período precisa abranger as atividades já cadastradas.");
        }
        if (corpo.has("fuso") && !corpo.getString("fuso").equals(evento.getFuso().getId()))
            throw new EventoInvalidoException(
                    "O fuso é definido na criação do evento e não pode ser alterado.");
        evento.definirLocalEFuso(
                corpo.optString("local", evento.getLocal()), evento.getFuso().getId());
        evento.editar(
                corpo.optString("titulo", null),
                corpo.optString("descricao", null),
                data(corpo, "inicio"),
                data(corpo, "fim"),
                modalidade(corpo));
        HttpJson.responder(exchange, 200, paraJson(repository.salvar(evento)));
    }

    private void transicionar(HttpExchange exchange, Long id, Consumer<Evento> transicao)
            throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);

        Optional<Evento> existente = repository.buscarPorId(id);
        if (existente.isEmpty()) {
            HttpJson.responder(exchange, 404, HttpJson.erro("Evento " + id + " não encontrado."));
            return;
        }
        Evento evento = existente.get();
        transicao.accept(evento);
        HttpJson.responder(exchange, 200, paraJson(repository.salvar(evento)));
    }

    private void remover(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);

        if (repository.buscarPorId(id).isEmpty()) {
            HttpJson.responder(exchange, 404, HttpJson.erro("Evento " + id + " não encontrado."));
            return;
        }
        if (repository.buscarPorId(id).orElseThrow().getStatus()
                != domain.evento.StatusEvento.RASCUNHO)
            throw new EventoInvalidoException(
                    "Somente rascunhos podem ser removidos. Encerre o evento para preservar o"
                            + " histórico.");
        repository.remover(id);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private LocalDateTime data(JSONObject corpo, String campo) {
        String valor = corpo.optString(campo, null);
        return valor == null ? null : LocalDateTime.parse(valor);
    }

    private Modalidade modalidade(JSONObject corpo) {
        String valor = corpo.optString("modalidade", null);
        if (valor == null) {
            throw new EventoInvalidoException("Modalidade do evento é obrigatória.");
        }
        try {
            return Modalidade.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new EventoInvalidoException("Modalidade inválida: " + valor);
        }
    }

    private JSONObject paraJson(Evento evento) {
        JSONObject json = new JSONObject();
        json.put("local", evento.getLocal());
        json.put("fuso", evento.getFuso().getId());
        json.put("id", evento.getId());
        json.put("titulo", evento.getTitulo());
        json.put("descricao", evento.getDescricao());
        json.put("inicio", evento.getInicio().toString());
        json.put("fim", evento.getFim().toString());
        json.put("modalidade", evento.getModalidade().name());
        json.put("status", evento.getStatus().name());
        return json;
    }

    private String[] caminho(HttpExchange exchange) {
        String resto = exchange.getRequestURI().getPath().replaceFirst("^/eventos/?", "");
        return resto.isBlank() ? new String[0] : resto.split("/");
    }
}
