package adapter.in.api;

import application.evento.EventoService;

import com.sun.net.httpserver.HttpExchange;

import domain.evento.Evento;
import domain.evento.EventoInvalidoException;
import domain.evento.Modalidade;
import domain.usuario.Papel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Um único contexto, "/eventos" — sem framework, quem resolve qual rota é este handler mesmo,
 * olhando pro método HTTP e pro caminho.
 *
 * <p>Leitura (GET) é pública (RF-10: visitante consulta sem login). Escrita exige Organizador ou
 * Administrador — verificado aqui no servidor, não só escondido na tela (RNF-06).
 */
public class EventoHttpHandler extends Endpoint {

    private static final Papel[] PODE_GERENCIAR_EVENTOS = {Papel.ORGANIZADOR, Papel.ADMINISTRADOR};

    private final EventoService service;
    private final Autenticador autenticador;

    public EventoHttpHandler(EventoService service, Autenticador autenticador) {
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
                publicar(exchange, id);
                return;
            }
            if (metodo.equals("POST") && partes[1].equals("encerrar")) {
                encerrar(exchange, id);
                return;
            }
        }

        HttpJson.responder(exchange, 404, HttpJson.erro("Rota não encontrada."));
    }

    private void listar(HttpExchange exchange) throws IOException {
        JSONArray json = new JSONArray();
        service.listar(autenticador.podeGerenciar(exchange))
                .forEach(evento -> json.put(paraJson(evento)));
        HttpJson.responder(exchange, 200, json);
    }

    private void criar(HttpExchange exchange) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Evento evento =
                service.criar(
                        corpo.optString("titulo", null),
                        corpo.optString("descricao", null),
                        data(corpo, "inicio"),
                        data(corpo, "fim"),
                        modalidade(corpo),
                        corpo.optString("local", "A definir"),
                        corpo.optString("fuso", "America/Sao_Paulo"));
        HttpJson.responder(exchange, 201, paraJson(evento));
    }

    private void buscar(HttpExchange exchange, Long id) throws IOException {
        var evento = service.buscar(id, autenticador.podeGerenciar(exchange));
        if (evento.isEmpty()) {
            HttpJson.responder(exchange, 404, HttpJson.erro("Evento " + id + " não encontrado."));
            return;
        }
        HttpJson.responder(exchange, 200, paraJson(evento.get()));
    }

    private void editar(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Evento atual =
                service.buscar(id, true)
                        .orElseThrow(
                                () -> new NoSuchElementException("Evento " + id + " não encontrado."));
        Evento evento =
                service.editar(
                        id,
                        corpo.optString("titulo", null),
                        corpo.optString("descricao", null),
                        data(corpo, "inicio"),
                        data(corpo, "fim"),
                        modalidade(corpo),
                        corpo.optString("local", atual.getLocal()),
                        corpo.optString("fuso", atual.getFuso().getId()));
        HttpJson.responder(exchange, 200, paraJson(evento));
    }

    private void publicar(HttpExchange exchange, long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);
        HttpJson.responder(exchange, 200, paraJson(service.publicar(id)));
    }

    private void encerrar(HttpExchange exchange, long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);
        HttpJson.responder(exchange, 200, paraJson(service.encerrar(id)));
    }

    private void remover(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_EVENTOS);
        service.remover(id);
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
