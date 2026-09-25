import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Único ponto de contato do desktop com o backend. Fala só HTTP — nunca importa
 * domain/application/adapter.out de lá (RNF-02).
 */
public class EventoApiClient {

    private static final String RAIZ = System.getProperty("api.url", "http://localhost:8080");
    private static final String EVENTOS_URL = RAIZ + "/eventos";

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    private String token;
    private String usuarioLogado;

    public String usuarioLogado() {
        return usuarioLogado;
    }

    public void login(String email, String senha) throws IOException, InterruptedException {
        JSONObject corpo = new JSONObject().put("email", email).put("senha", senha);
        HttpResponse<String> resposta =
                enviarSemAutenticar(
                        HttpRequest.newBuilder(URI.create(RAIZ + "/login"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString())));

        JSONObject json = new JSONObject(resposta.body());
        this.token = json.getString("token");
        this.usuarioLogado = json.getString("nome") + " (" + json.getString("papel") + ")";
    }

    public void cadastrar(String nome, String email, String senha)
            throws IOException, InterruptedException {
        JSONObject corpo =
                new JSONObject().put("nome", nome).put("email", email).put("senha", senha);
        enviarSemAutenticar(
                HttpRequest.newBuilder(URI.create(RAIZ + "/usuarios"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(corpo.toString())));
    }

    public List<JSONObject> listar() throws IOException, InterruptedException {
        HttpResponse<String> resposta =
                enviarAutenticado(HttpRequest.newBuilder(URI.create(EVENTOS_URL)).GET());

        JSONArray json = new JSONArray(resposta.body());
        List<JSONObject> eventos = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            eventos.add(json.getJSONObject(i));
        }
        return eventos;
    }

    public JSONObject criar(
            String titulo, String descricao, String inicio, String fim, String modalidade)
            throws IOException, InterruptedException {
        JSONObject corpo =
                new JSONObject()
                        .put("titulo", titulo)
                        .put("descricao", descricao)
                        .put("inicio", inicio)
                        .put("fim", fim)
                        .put("modalidade", modalidade);

        HttpResponse<String> resposta =
                enviarAutenticado(
                        HttpRequest.newBuilder(URI.create(EVENTOS_URL))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString())));

        return new JSONObject(resposta.body());
    }

    public void remover(long id) throws IOException, InterruptedException {
        enviarAutenticado(HttpRequest.newBuilder(URI.create(EVENTOS_URL + "/" + id)).DELETE());
    }

    /** Métodos reutilizáveis para as próximas telas; nunca acessam o banco. */
    public String requisicao(String metodo, String caminho, JSONObject corpo)
            throws IOException, InterruptedException {
        var builder =
                HttpRequest.newBuilder(URI.create(RAIZ + caminho))
                        .header("Content-Type", "application/json");
        builder.method(
                metodo,
                corpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo.toString()));
        return enviarAutenticado(builder).body();
    }

    public JSONArray atividades(long eventoId) throws IOException, InterruptedException {
        return new JSONArray(requisicao("GET", "/atividades?eventoId=" + eventoId, null));
    }

    public JSONObject criarAtividade(JSONObject corpo) throws IOException, InterruptedException {
        return new JSONObject(requisicao("POST", "/atividades", corpo));
    }

    public void publicar(long eventoId) throws IOException, InterruptedException {
        requisicao("POST", "/eventos/" + eventoId + "/publicar", new JSONObject());
    }

    public void encerrar(long eventoId) throws IOException, InterruptedException {
        requisicao("POST", "/eventos/" + eventoId + "/encerrar", new JSONObject());
    }

    public JSONObject gerarQr(long atividadeId, String tipo)
            throws IOException, InterruptedException {
        return new JSONObject(
                requisicao(
                        "POST",
                        "/frequencia/" + atividadeId + "/codigos",
                        new JSONObject().put("tipo", tipo)));
    }

    public void registrarManual(
            long atividadeId, long usuarioId, boolean presente, String justificativa)
            throws IOException, InterruptedException {
        requisicao(
                "POST",
                "/frequencia/" + atividadeId + "/manual",
                new JSONObject()
                        .put("usuarioId", usuarioId)
                        .put("presente", presente)
                        .put("justificativa", justificativa));
    }

    public String relatorio(long eventoId, String tipo, boolean csv)
            throws IOException, InterruptedException {
        return requisicao(
                "GET",
                "/relatorios/" + tipo + "?eventoId=" + eventoId + (csv ? "&formato=csv" : ""),
                null);
    }

    private HttpResponse<String> enviarAutenticado(HttpRequest.Builder requisicao)
            throws IOException, InterruptedException {
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return enviarSemAutenticar(requisicao);
    }

    private HttpResponse<String> enviarSemAutenticar(HttpRequest.Builder requisicao)
            throws IOException, InterruptedException {
        HttpResponse<String> resposta =
                http.send(
                        requisicao.timeout(java.time.Duration.ofSeconds(15)).build(),
                        HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() >= 400) {
            String mensagem =
                    resposta.body().isBlank()
                            ? "A API respondeu " + resposta.statusCode()
                            : new JSONObject(resposta.body()).optString("erro", "Erro na API.");
            throw new IOException(mensagem);
        }
        return resposta;
    }
}
