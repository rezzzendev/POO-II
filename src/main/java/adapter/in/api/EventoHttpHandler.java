package adapter.in.api;

import application.evento.EventoRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import domain.evento.Evento;
import domain.evento.EventoInvalidoException;
import domain.evento.Modalidade;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Um único contexto, "/eventos" — sem framework, quem resolve qual rota é
 * este handler mesmo, olhando pro método HTTP e pro caminho.
 */
public class EventoHttpHandler implements HttpHandler {

    private final EventoRepository repository;

    public EventoHttpHandler(EventoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (exchange.getRequestMethod().equals("OPTIONS")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            rotear(exchange);
        } catch (EventoInvalidoException | DateTimeParseException e) {
            responder(exchange, 400, erro(e.getMessage()));
        } catch (NumberFormatException e) {
            responder(exchange, 400, erro("Id do evento precisa ser um número."));
        } catch (Exception e) {
            responder(exchange, 500, erro("Erro interno: " + e.getMessage()));
        }
    }

    private void rotear(HttpExchange exchange) throws IOException {
        String metodo = exchange.getRequestMethod();
        String[] partes = caminho(exchange);

        if (partes.length == 0) {
            if (metodo.equals("GET")) { listar(exchange); return; }
            if (metodo.equals("POST")) { criar(exchange); return; }
        } else if (partes.length == 1) {
            Long id = Long.valueOf(partes[0]);
            if (metodo.equals("GET")) { buscar(exchange, id); return; }
            if (metodo.equals("PUT")) { editar(exchange, id); return; }
            if (metodo.equals("DELETE")) { remover(exchange, id); return; }
        } else if (partes.length == 2) {
            Long id = Long.valueOf(partes[0]);
            if (metodo.equals("POST") && partes[1].equals("publicar")) { transicionar(exchange, id, Evento::publicar); return; }
            if (metodo.equals("POST") && partes[1].equals("encerrar")) { transicionar(exchange, id, Evento::encerrar); return; }
        }

        responder(exchange, 404, erro("Rota não encontrada."));
    }

    private void listar(HttpExchange exchange) throws IOException {
        List<Evento> eventos = repository.listarTodos();
        JSONArray json = new JSONArray();
        eventos.forEach(evento -> json.put(paraJson(evento)));
        responder(exchange, 200, json);
    }

    private void criar(HttpExchange exchange) throws IOException {
        JSONObject corpo = lerCorpo(exchange);
        Evento evento = Evento.novo(
                corpo.optString("titulo", null),
                corpo.optString("descricao", null),
                data(corpo, "inicio"),
                data(corpo, "fim"),
                modalidade(corpo)
        );
        responder(exchange, 201, paraJson(repository.salvar(evento)));
    }

    private void buscar(HttpExchange exchange, Long id) throws IOException {
        Optional<Evento> evento = repository.buscarPorId(id);
        if (evento.isEmpty()) {
            responder(exchange, 404, erro("Evento " + id + " não encontrado."));
            return;
        }
        responder(exchange, 200, paraJson(evento.get()));
    }

    private void editar(HttpExchange exchange, Long id) throws IOException {
        Optional<Evento> existente = repository.buscarPorId(id);
        if (existente.isEmpty()) {
            responder(exchange, 404, erro("Evento " + id + " não encontrado."));
            return;
        }
        JSONObject corpo = lerCorpo(exchange);
        Evento evento = existente.get();
        evento.editar(
                corpo.optString("titulo", null),
                corpo.optString("descricao", null),
                data(corpo, "inicio"),
                data(corpo, "fim"),
                modalidade(corpo)
        );
        responder(exchange, 200, paraJson(repository.salvar(evento)));
    }

    private void transicionar(HttpExchange exchange, Long id, Consumer<Evento> transicao) throws IOException {
        Optional<Evento> existente = repository.buscarPorId(id);
        if (existente.isEmpty()) {
            responder(exchange, 404, erro("Evento " + id + " não encontrado."));
            return;
        }
        Evento evento = existente.get();
        transicao.accept(evento);
        responder(exchange, 200, paraJson(repository.salvar(evento)));
    }

    private void remover(HttpExchange exchange, Long id) throws IOException {
        if (repository.buscarPorId(id).isEmpty()) {
            responder(exchange, 404, erro("Evento " + id + " não encontrado."));
            return;
        }
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

    private JSONObject lerCorpo(HttpExchange exchange) throws IOException {
        try (InputStream entrada = exchange.getRequestBody()) {
            String texto = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            return texto.isBlank() ? new JSONObject() : new JSONObject(texto);
        }
    }

    private void responder(HttpExchange exchange, int status, Object corpo) throws IOException {
        byte[] bytes = corpo.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private JSONObject erro(String mensagem) {
        return new JSONObject().put("erro", mensagem);
    }
}
