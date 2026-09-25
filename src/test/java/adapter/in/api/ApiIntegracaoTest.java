package adapter.in.api;

import static org.junit.jupiter.api.Assertions.*;

import adapter.out.persistence.*;
import adapter.out.persistence.usuario.UsuarioRepositoryJdbc;

import com.sun.net.httpserver.HttpServer;

import domain.usuario.*;

import org.json.*;
import org.junit.jupiter.api.*;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;

class ApiIntegracaoTest {
    HttpServer servidor;
    String base, admin, participante;
    long usuarioId;
    final HttpClient http = HttpClient.newHttpClient();
    final Instant agora = Instant.parse("2026-09-24T15:00:00Z");

    @BeforeEach
    void iniciar() throws Exception {
        System.setProperty("db.url", "jdbc:h2:mem:api_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        new UsuarioRepositoryJdbc()
                .salvar(Usuario.novo("Admin", "admin@test.local", "senha", Papel.ADMINISTRADOR));
        servidor = ServidorApi.criar(0, Clock.fixed(agora, ZoneOffset.UTC));
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        admin =
                json(post(
                                "/login",
                                null,
                                new JSONObject()
                                        .put("email", "admin@test.local")
                                        .put("senha", "senha"),
                                200))
                        .getString("token");
        usuarioId =
                json(post(
                                "/usuarios",
                                null,
                                new JSONObject()
                                        .put("nome", "Pessoa")
                                        .put("email", "p@test.local")
                                        .put("senha", "senha")
                                        .put("papel", "ADMINISTRADOR"),
                                201))
                        .getLong("id");
        participante =
                json(post(
                                "/login",
                                null,
                                new JSONObject().put("email", "p@test.local").put("senha", "senha"),
                                200))
                        .getString("token");
    }

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
        System.setProperty("db.url", "jdbc:h2:mem:testes;DB_CLOSE_DELAY=-1");
    }

    HttpResponse<String> req(String m, String p, String token, Object b) throws Exception {
        var r =
                HttpRequest.newBuilder(URI.create(base + p))
                        .header("Content-Type", "application/json");
        if (token != null) r.header("Authorization", "Bearer " + token);
        return http.send(
                r.method(
                                m,
                                b == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(b.toString()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> post(String p, String t, Object b, int status) throws Exception {
        var r = req("POST", p, t, b);
        assertEquals(status, r.statusCode(), r.body());
        return r;
    }

    JSONObject json(HttpResponse<String> r) {
        return new JSONObject(r.body());
    }

    long evento() throws Exception {
        return json(post(
                        "/eventos",
                        admin,
                        new JSONObject()
                                .put("titulo", "Evento")
                                .put("inicio", "2026-09-24T08:00:00")
                                .put("fim", "2026-09-25T18:00:00")
                                .put("modalidade", "PRESENCIAL")
                                .put("local", "Campus")
                                .put("fuso", "America/Sao_Paulo"),
                        201))
                .getLong("id");
    }

    long atividade(long e, String titulo, String inicio, String fim, int capacidade)
            throws Exception {
        return json(post(
                        "/atividades",
                        admin,
                        new JSONObject()
                                .put("eventoId", e)
                                .put("titulo", titulo)
                                .put("tipo", "Oficina")
                                .put("trilha", "Java")
                                .put("local", titulo)
                                .put("inicio", inicio)
                                .put("fim", fim)
                                .put("capacidade", capacidade),
                        201))
                .getLong("id");
    }

    void publicar(long e) throws Exception {
        post("/eventos/" + e + "/publicar", admin, new JSONObject(), 200);
    }

    long inscrever(long e, long a) throws Exception {
        return json(post(
                        "/inscricoes",
                        participante,
                        new JSONObject()
                                .put("eventoId", e)
                                .put("atividadeIds", new JSONArray().put(a)),
                        201))
                .getLong("id");
    }

    @Test
    void fluxoCompletoQrManualQuestionarioRelatorio() throws Exception {
        long e = evento(),
                a = atividade(e, "Oficina", "2026-09-24T11:00:00", "2026-09-24T13:00:00", 2);
        publicar(e);
        inscrever(e, a);
        assertEquals(1, new JSONArray(req("GET", "/agenda", participante, null).body()).length());
        var q =
                json(
                        post(
                                "/questionarios",
                                admin,
                                new JSONObject()
                                        .put("atividadeId", a)
                                        .put("titulo", "Avaliação")
                                        .put(
                                                "perguntas",
                                                new JSONArray()
                                                        .put(
                                                                new JSONObject()
                                                                        .put("enunciado", "Nota")
                                                                        .put("tipo", "ESCALA")
                                                                        .put("minimo", 1)
                                                                        .put("maximo", 5))
                                                        .put(
                                                                new JSONObject()
                                                                        .put(
                                                                                "enunciado",
                                                                                "Comentário")
                                                                        .put("tipo", "TEXTO"))
                                                        .put(
                                                                new JSONObject()
                                                                        .put(
                                                                                "enunciado",
                                                                                "Recomenda?")
                                                                        .put(
                                                                                "tipo",
                                                                                "ESCOLHA_UNICA")
                                                                        .put(
                                                                                "opcoes",
                                                                                List.of(
                                                                                        "Sim",
                                                                                        "Não")))),
                                201));
        long qid = q.getLong("id");
        var ps = q.getJSONArray("perguntas");
        JSONObject respostas =
                new JSONObject()
                        .put(
                                "respostas",
                                new JSONObject()
                                        .put("" + ps.getJSONObject(0).getLong("id"), 5)
                                        .put("" + ps.getJSONObject(1).getLong("id"), "Ótimo")
                                        .put("" + ps.getJSONObject(2).getLong("id"), "Sim"));
        post("/questionarios/" + qid + "/respostas", participante, respostas, 400);
        var codigo =
                json(
                        post(
                                "/frequencia/" + a + "/codigos",
                                admin,
                                new JSONObject().put("tipo", "CHECK_IN"),
                                201));
        post(
                "/frequencia/qr",
                participante,
                new JSONObject().put("imagemBase64", codigo.getString("imagemBase64")),
                201);
        post(
                "/frequencia/qr",
                participante,
                new JSONObject().put("token", codigo.getString("token")),
                400);
        assertTrue(json(req("GET", "/frequencia/" + a, participante, null)).getBoolean("presente"));
        post(
                "/frequencia/" + a + "/manual",
                admin,
                new JSONObject()
                        .put("usuarioId", usuarioId)
                        .put("presente", false)
                        .put("justificativa", "Correção conferida"),
                201);
        assertFalse(
                json(req("GET", "/frequencia/" + a, participante, null)).getBoolean("presente"));
        post(
                "/frequencia/" + a + "/manual",
                admin,
                new JSONObject()
                        .put("usuarioId", usuarioId)
                        .put("presente", true)
                        .put("justificativa", "Presença confirmada"),
                201);
        assertEquals(
                3,
                json(req("GET", "/frequencia/" + a, participante, null))
                        .getJSONArray("registros")
                        .length());
        assertEquals(
                400,
                req(
                                "PUT",
                                "/frequencia/" + a + "/politica",
                                admin,
                                new JSONObject().put("politica", "MANUAL"))
                        .statusCode());
        post("/questionarios/" + qid + "/respostas", participante, respostas, 201);
        post("/questionarios/" + qid + "/respostas", participante, respostas, 400);
        assertEquals(
                1,
                json(req("GET", "/questionarios/" + qid + "/resultados", admin, null))
                        .getInt("total"));
        assertEquals(
                403,
                req("GET", "/questionarios/" + qid + "/resultados", participante, null)
                        .statusCode());
        assertEquals(
                1,
                json(req("GET", "/relatorios/frequencia?eventoId=" + e, admin, null))
                        .getInt("presentes"));
        var csv = req("GET", "/relatorios/frequencia?eventoId=" + e + "&formato=csv", admin, null);
        assertEquals(200, csv.statusCode());
        assertTrue(csv.body().contains("Pessoa"));
        servidor.stop(0);
        servidor = ServidorApi.criar(0, Clock.fixed(agora, ZoneOffset.UTC));
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        admin =
                json(post(
                                "/login",
                                null,
                                new JSONObject()
                                        .put("email", "admin@test.local")
                                        .put("senha", "senha"),
                                200))
                        .getString("token");
        assertEquals(
                1,
                json(req("GET", "/questionarios/" + qid + "/resultados", admin, null))
                        .getInt("total"));
    }

    @Test
    void vagasConflitoVinculoCancelamentoEAtomicidade() throws Exception {
        long e = evento(),
                a = atividade(e, "A", "2026-09-24T11:00:00", "2026-09-24T13:00:00", 1),
                b = atividade(e, "B", "2026-09-24T12:00:00", "2026-09-24T14:00:00", 1);
        publicar(e);
        post(
                "/inscricoes",
                participante,
                new JSONObject().put("eventoId", e).put("atividadeIds", List.of(a, b)),
                400);
        long outro = evento(),
                c = atividade(outro, "C", "2026-09-24T14:00:00", "2026-09-24T15:00:00", 1);
        publicar(outro);
        post(
                "/inscricoes",
                participante,
                new JSONObject().put("eventoId", e).put("atividadeIds", List.of(c)),
                400);
        long i = inscrever(e, a);
        post(
                "/inscricoes",
                admin,
                new JSONObject().put("eventoId", e).put("atividadeIds", List.of(a)),
                400);
        post(
                "/inscricoes/" + i + "/cancelar",
                participante,
                new JSONObject(),
                400); // clock after configured deadline
        assertEquals(
                400,
                req(
                                "PUT",
                                "/regras-inscricao/" + e,
                                admin,
                                new JSONObject()
                                        .put("escolherAtividades", false)
                                        .put("controlarVagas", false)
                                        .put("prazoCancelamento", "2026-09-25T15:00:00"))
                        .statusCode());
        assertEquals(
                1,
                json(req("GET", "/relatorios/inscritos?eventoId=" + e, admin, null))
                        .getInt("total"));
    }

    @Test
    void cancelamentoNoPrazoLiberaVaga() throws Exception {
        long e = evento(), a = atividade(e, "A", "2026-09-24T11:00:00", "2026-09-24T13:00:00", 1);
        assertEquals(
                200,
                req(
                                "PUT",
                                "/regras-inscricao/" + e,
                                admin,
                                new JSONObject()
                                        .put("escolherAtividades", true)
                                        .put("controlarVagas", true)
                                        .put("prazoCancelamento", "2026-09-25T15:00:00"))
                        .statusCode());
        publicar(e);
        long i = inscrever(e, a);
        post("/inscricoes/" + i + "/cancelar", participante, new JSONObject(), 200);
        assertEquals(
                400,
                req(
                                "PUT",
                                "/inscricoes/" + i + "/atividades",
                                participante,
                                new JSONObject().put("atividadeIds", List.of(a)))
                        .statusCode());
        post(
                "/inscricoes",
                admin,
                new JSONObject().put("eventoId", e).put("atividadeIds", List.of(a)),
                201);
    }

    @Test
    void autorizacaoRascunhoEMalformedJson() throws Exception {
        long e = evento();
        assertEquals(0, new JSONArray(req("GET", "/eventos", null, null).body()).length());
        assertEquals(404, req("GET", "/eventos/" + e, null, null).statusCode());
        post("/eventos", participante, new JSONObject(), 403);
        post("/eventos", null, new JSONObject(), 401);
        post("/usuarios", null, "{malformado", 400);
        post(
                "/usuarios",
                null,
                new JSONObject()
                        .put("nome", "Outro")
                        .put("email", " P@TEST.LOCAL ")
                        .put("senha", "senha"),
                400);
        assertEquals(
                "PARTICIPANTE",
                json(req("GET", "/usuarios/me", participante, null)).getString("papel"));
        assertEquals(
                403,
                req(
                                "PUT",
                                "/usuarios/" + usuarioId + "/papel",
                                participante,
                                new JSONObject().put("papel", "ADMINISTRADOR"))
                        .statusCode());
        assertEquals(
                403,
                req("GET", "/relatorios/inscritos?eventoId=" + e, participante, null).statusCode());
    }

    @Test
    void inscricaoSomenteEventoEFrequenciaManual() throws Exception {
        long e = evento(), a = atividade(e, "A", "2026-09-24T11:00:00", "2026-09-24T13:00:00", 1);
        assertEquals(
                200,
                req(
                                "PUT",
                                "/regras-inscricao/" + e,
                                admin,
                                new JSONObject()
                                        .put("escolherAtividades", false)
                                        .put("controlarVagas", false)
                                        .put("prazoCancelamento", "2026-09-25T15:00:00"))
                        .statusCode());
        assertEquals(
                200,
                req(
                                "PUT",
                                "/frequencia/" + a + "/politica",
                                admin,
                                new JSONObject().put("politica", "MANUAL"))
                        .statusCode());
        publicar(e);
        post(
                "/inscricoes",
                participante,
                new JSONObject().put("eventoId", e).put("atividadeIds", List.of(a)),
                400);
        post("/inscricoes", participante, new JSONObject().put("eventoId", e), 201);
        post("/frequencia/" + a + "/codigos", admin, new JSONObject().put("tipo", "CHECK_IN"), 400);
        post(
                "/frequencia/" + a + "/manual",
                participante,
                new JSONObject()
                        .put("usuarioId", usuarioId)
                        .put("presente", true)
                        .put("justificativa", "Tentativa"),
                403);
        post(
                "/frequencia/" + a + "/manual",
                admin,
                new JSONObject()
                        .put("usuarioId", usuarioId)
                        .put("presente", true)
                        .put("justificativa", "Lista conferida"),
                201);
        assertTrue(json(req("GET", "/frequencia/" + a, participante, null)).getBoolean("presente"));
    }

    @Test
    void duasRequisicoesSimultaneasNaoExcedemCapacidade() throws Exception {
        long e = evento(), a = atividade(e, "A", "2026-09-24T11:00:00", "2026-09-24T13:00:00", 1);
        publicar(e);
        var corpo = new JSONObject().put("eventoId", e).put("atividadeIds", List.of(a)).toString();
        var r1 =
                HttpRequest.newBuilder(URI.create(base + "/inscricoes"))
                        .header("Authorization", "Bearer " + admin)
                        .POST(HttpRequest.BodyPublishers.ofString(corpo))
                        .build();
        var r2 =
                HttpRequest.newBuilder(URI.create(base + "/inscricoes"))
                        .header("Authorization", "Bearer " + participante)
                        .POST(HttpRequest.BodyPublishers.ofString(corpo))
                        .build();
        var f1 = http.sendAsync(r1, HttpResponse.BodyHandlers.ofString());
        var f2 = http.sendAsync(r2, HttpResponse.BodyHandlers.ofString());
        assertEquals(Set.of(201, 400), Set.of(f1.get().statusCode(), f2.get().statusCode()));
        assertEquals(
                1,
                json(req("GET", "/relatorios/inscritos?eventoId=" + e, admin, null))
                        .getInt("confirmados"));
    }

    @Test
    void qrExpiradoNaoRegistra() throws Exception {
        long e = evento(), a = atividade(e, "A", "2026-09-24T11:00:00", "2026-09-24T13:00:00", 2);
        publicar(e);
        inscrever(e, a);
        var codigo =
                json(
                        post(
                                "/frequencia/" + a + "/codigos",
                                admin,
                                new JSONObject().put("tipo", "CHECK_IN"),
                                201));
        servidor.stop(0);
        servidor = ServidorApi.criar(0, Clock.fixed(agora.plusSeconds(300), ZoneOffset.UTC));
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
        participante =
                json(post(
                                "/login",
                                null,
                                new JSONObject().put("email", "p@test.local").put("senha", "senha"),
                                200))
                        .getString("token");
        var resposta =
                post(
                        "/frequencia/qr",
                        participante,
                        new JSONObject().put("token", codigo.getString("token")),
                        400);
        assertTrue(resposta.body().contains("expirado"));
        assertEquals(
                0,
                json(req("GET", "/frequencia/" + a, participante, null))
                        .getJSONArray("registros")
                        .length());
    }
}
