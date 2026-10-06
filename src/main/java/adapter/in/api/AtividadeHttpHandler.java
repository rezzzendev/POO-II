package adapter.in.api;

import application.atividade.AtividadeService;

import com.sun.net.httpserver.HttpExchange;

import domain.atividade.Atividade;
import domain.atividade.AtividadeInvalidaException;
import domain.atividade.VinculoPessoa;
import domain.usuario.Papel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contexto "/atividades". Listar aceita filtros combináveis por query string (RF-09): eventoId,
 * data, trilha, tipo, local — todos opcionais.
 */
public class AtividadeHttpHandler extends Endpoint {

    private static final Papel[] PODE_GERENCIAR_ATIVIDADES = {
        Papel.ORGANIZADOR, Papel.ADMINISTRADOR
    };

    private final AtividadeService service;
    private final Autenticador autenticador;

    public AtividadeHttpHandler(AtividadeService service, Autenticador autenticador) {
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

        boolean gestor = autenticador.podeGerenciar(exchange);
        var encontradas = service.listar(eventoId, data, trilha, tipo, local, gestor);
        Integer pagina = parametroInteiro(exchange, "pagina");
        Integer tamanho = parametroInteiro(exchange, "tamanho");

        // Sem os dois parâmetros, conserva o contrato antigo usado pelo desktop.
        if (pagina == null && tamanho == null) {
            JSONArray json = new JSONArray();
            encontradas.forEach(atividade -> json.put(paraJson(atividade)));
            HttpJson.responder(exchange, 200, json);
            return;
        }
        if (pagina == null || tamanho == null)
            throw new IllegalArgumentException("Informe pagina e tamanho juntos.");
        if (pagina < 1) throw new IllegalArgumentException("A página deve ser maior que zero.");
        if (tamanho < 1 || tamanho > 50)
            throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e 50.");

        int total = encontradas.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamanho));
        int inicio = Math.min((pagina - 1) * tamanho, total);
        int fim = Math.min(inicio + tamanho, total);
        JSONArray itens = new JSONArray();
        encontradas.subList(inicio, fim).forEach(atividade -> itens.put(paraJson(atividade)));
        HttpJson.responder(
                exchange,
                200,
                new JSONObject()
                        .put("itens", itens)
                        .put("pagina", pagina)
                        .put("tamanho", tamanho)
                        .put("total", total)
                        .put("totalPaginas", totalPaginas));
    }

    private void criar(HttpExchange exchange) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Atividade atividade =
                service.criar(
                        eventoId(corpo),
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
        HttpJson.responder(exchange, 201, paraJson(atividade));
    }

    private void buscar(HttpExchange exchange, Long id) throws IOException {
        var atividade = service.buscar(id, autenticador.podeGerenciar(exchange));
        if (atividade.isEmpty()) {
            HttpJson.responder(
                    exchange, 404, HttpJson.erro("Atividade " + id + " não encontrada."));
            return;
        }
        HttpJson.responder(exchange, 200, paraJson(atividade.get()));
    }

    private void editar(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        Atividade atividade =
                service.editar(
                        id,
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
        HttpJson.responder(exchange, 200, paraJson(atividade));
    }

    private void remover(HttpExchange exchange, Long id) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);
        service.remover(id);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void listarPessoas(HttpExchange exchange, Long atividadeId) throws IOException {
        JSONArray json = new JSONArray();
        service.listarPessoas(atividadeId, autenticador.podeGerenciar(exchange))
                .forEach(vinculo -> json.put(paraJson(vinculo)));
        HttpJson.responder(exchange, 200, json);
    }

    private void vincularPessoa(HttpExchange exchange, Long atividadeId) throws IOException {
        autenticador.exigir(exchange, PODE_GERENCIAR_ATIVIDADES);

        JSONObject corpo = HttpJson.lerCorpo(exchange);
        VinculoPessoa vinculo =
                corpo.has("email")
                        ? service.vincularPessoa(
                                atividadeId,
                                corpo.optString("email", null),
                                corpo.optString("papel", null))
                        : service.vincularPessoa(
                                atividadeId,
                                corpo.getLong("usuarioId"),
                                corpo.optString("papel", null));
        HttpJson.responder(exchange, 201, paraJson(vinculo));
    }

    private long eventoId(JSONObject corpo) {
        if (!corpo.has("eventoId")) {
            throw new AtividadeInvalidaException("eventoId é obrigatório.");
        }
        return corpo.getLong("eventoId");
    }

    private Long parametroLong(HttpExchange exchange, String nome) {
        String valor = parametroTexto(exchange, nome);
        return valor == null ? null : Long.valueOf(valor);
    }

    private Integer parametroInteiro(HttpExchange exchange, String nome) {
        String valor = parametroTexto(exchange, nome);
        return valor == null ? null : Integer.valueOf(valor);
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
