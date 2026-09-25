package adapter.in.api;

import application.avaliacao.AvaliacaoService;

import com.sun.net.httpserver.HttpExchange;

import domain.avaliacao.*;
import domain.usuario.Papel;

import org.json.*;

import java.io.IOException;
import java.util.*;

public class AvaliacaoHttpHandler extends Endpoint {
    private final AvaliacaoService service;
    private final Autenticador auth;

    public AvaliacaoHttpHandler(AvaliacaoService s, Autenticador a) {
        service = s;
        auth = a;
    }

    protected void executar(HttpExchange e) throws IOException {
        String[] p = partes(e);
        String m = e.getRequestMethod();
        if (p.length == 1 && m.equals("POST")) {
            gerente(e);
            JSONObject b = HttpJson.lerCorpo(e);
            List<Pergunta> perguntas = new ArrayList<>();
            JSONArray entrada = b.getJSONArray("perguntas");
            for (int indice = 0; indice < entrada.length(); indice++) {
                JSONObject v = entrada.getJSONObject(indice);
                TipoResposta tipo =
                        switch (v.getString("tipo")) {
                            case "TEXTO" -> new Texto();
                            case "ESCALA" -> new Escala(v.getInt("minimo"), v.getInt("maximo"));
                            case "ESCOLHA_UNICA" ->
                                    new EscolhaUnica(opcoes(v.getJSONArray("opcoes")));
                            default ->
                                    throw new IllegalArgumentException(
                                            "Tipo de resposta inválido.");
                        };
                perguntas.add(new Pergunta(0, v.getString("enunciado"), tipo));
            }
            HttpJson.responder(
                    e,
                    201,
                    json(
                            service.criar(
                                    new Questionario(
                                            0,
                                            b.getLong("atividadeId"),
                                            b.getString("titulo"),
                                            perguntas))));
            return;
        }
        if (p.length == 1 && m.equals("GET")) {
            auth.exigir(e);
            JSONArray lista = new JSONArray();
            service.listar(Long.parseLong(parametro(e, "atividadeId")))
                    .forEach(q -> lista.put(json(q)));
            HttpJson.responder(e, 200, lista);
            return;
        }
        if (p.length >= 2) {
            long id = Long.parseLong(p[1]);
            if (p.length == 2 && m.equals("GET")) {
                auth.exigir(e);
                HttpJson.responder(e, 200, json(service.buscar(id)));
                return;
            }
            if (p.length == 3 && p[2].equals("respostas") && m.equals("POST")) {
                long u = auth.exigir(e).getId();
                JSONObject b = HttpJson.lerCorpo(e).getJSONObject("respostas");
                Map<Long, String> valores = new LinkedHashMap<>();
                for (String key : b.keySet()) {
                    Object valor = b.get(key);
                    if (!(valor instanceof String) && !(valor instanceof Number))
                        throw new IllegalArgumentException("Resposta deve ser texto ou número.");
                    valores.put(Long.parseLong(key), valor.toString());
                }
                service.responder(id, u, valores);
                HttpJson.responder(
                        e, 201, new JSONObject().put("mensagem", "Avaliação registrada."));
                return;
            }
            if (p.length == 3 && p[2].equals("resultados") && m.equals("GET")) {
                gerente(e);
                List<Avaliacao> respostas = service.resultados(id);
                JSONArray resumo = new JSONArray(), detalhes = new JSONArray();
                for (var resultado : service.resumo(id)) {
                    JSONObject item =
                            new JSONObject()
                                    .put("perguntaId", resultado.perguntaId())
                                    .put("enunciado", resultado.enunciado())
                                    .put("distribuicao", new JSONObject(resultado.distribuicao()))
                                    .put("comentarios", resultado.comentarios());
                    if (resultado.media() != null) item.put("media", resultado.media());
                    resumo.put(item);
                }
                for (Avaliacao a : respostas)
                    detalhes.put(
                            new JSONObject()
                                    .put("usuarioId", a.usuarioId())
                                    .put("instante", a.instante().toString())
                                    .put("respostas", new JSONObject(a.respostas())));
                HttpJson.responder(
                        e,
                        200,
                        new JSONObject()
                                .put("total", respostas.size())
                                .put("politicaIdentificacao", Questionario.POLITICA)
                                .put("perguntas", resumo)
                                .put("avaliacoes", detalhes));
                return;
            }
        }
        naoEncontrado(e);
    }

    private List<String> opcoes(JSONArray entrada) {
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < entrada.length(); i++) resultado.add(entrada.getString(i));
        return resultado;
    }

    private void gerente(HttpExchange e) {
        auth.exigir(e, Papel.ADMINISTRADOR, Papel.ORGANIZADOR);
    }

    private JSONObject json(Questionario q) {
        JSONArray perguntas = new JSONArray();
        for (Pergunta p : q.perguntas()) {
            JSONObject j =
                    new JSONObject()
                            .put("id", p.id())
                            .put("enunciado", p.enunciado())
                            .put("tipo", p.tipo().nome());
            if (p.tipo() instanceof Escala v) j.put("minimo", v.minimo()).put("maximo", v.maximo());
            if (p.tipo() instanceof EscolhaUnica v) j.put("opcoes", v.opcoes());
            perguntas.put(j);
        }
        return new JSONObject()
                .put("id", q.id())
                .put("atividadeId", q.atividadeId())
                .put("titulo", q.titulo())
                .put("politicaIdentificacao", Questionario.POLITICA)
                .put("perguntas", perguntas);
    }
}
