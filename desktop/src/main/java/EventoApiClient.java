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
 * Único ponto de contato do desktop com o backend. Fala só HTTP — nunca
 * importa domain/application/adapter.out de lá (RNF-02).
 */
public class EventoApiClient {

    private static final String BASE_URL = "http://localhost:8080/eventos";

    private final HttpClient http = HttpClient.newHttpClient();

    public List<JSONObject> listar() throws IOException, InterruptedException {
        HttpResponse<String> resposta = enviar(HttpRequest.newBuilder(URI.create(BASE_URL)).GET());

        JSONArray json = new JSONArray(resposta.body());
        List<JSONObject> eventos = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            eventos.add(json.getJSONObject(i));
        }
        return eventos;
    }

    public JSONObject criar(String titulo, String descricao, String inicio, String fim, String modalidade)
            throws IOException, InterruptedException {
        JSONObject corpo = new JSONObject()
                .put("titulo", titulo)
                .put("descricao", descricao)
                .put("inicio", inicio)
                .put("fim", fim)
                .put("modalidade", modalidade);

        HttpResponse<String> resposta = enviar(HttpRequest.newBuilder(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo.toString())));

        return new JSONObject(resposta.body());
    }

    public void remover(long id) throws IOException, InterruptedException {
        enviar(HttpRequest.newBuilder(URI.create(BASE_URL + "/" + id)).DELETE());
    }

    private HttpResponse<String> enviar(HttpRequest.Builder requisicao) throws IOException, InterruptedException {
        HttpResponse<String> resposta = http.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() >= 400) {
            String mensagem = resposta.body().isBlank()
                    ? "A API respondeu " + resposta.statusCode()
                    : new JSONObject(resposta.body()).optString("erro", "Erro na API.");
            throw new IOException(mensagem);
        }
        return resposta;
    }
}
