package adapter.out.persistence;

import application.avaliacao.AvaliacaoRepository;

import domain.RegraViolada;
import domain.avaliacao.*;

import java.sql.*;
import java.time.*;
import java.util.*;

public class AvaliacaoRepositoryJdbc implements AvaliacaoRepository {
    public Questionario salvar(Questionario q) {
        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            long id =
                    Sql.executar(
                            c,
                            "INSERT INTO questionarios(atividade_id,titulo,politica_identificacao)"
                                    + " VALUES (?,?,?)",
                            q.atividadeId(),
                            q.titulo(),
                            Questionario.POLITICA);
            for (Pergunta p : q.perguntas()) {
                int min = p.tipo() instanceof Escala e ? e.minimo() : 0,
                        max = p.tipo() instanceof Escala e ? e.maximo() : 0;
                long pid =
                        Sql.executar(
                                c,
                                "INSERT INTO"
                                        + " perguntas(questionario_id,enunciado,tipo,minimo,maximo)"
                                        + " VALUES (?,?,?,?,?)",
                                id,
                                p.enunciado(),
                                p.tipo().nome(),
                                min,
                                max);
                if (p.tipo() instanceof EscolhaUnica escolha)
                    for (int i = 0; i < escolha.opcoes().size(); i++)
                        Sql.executar(
                                c,
                                "INSERT INTO opcoes_pergunta VALUES (?,?,?)",
                                pid,
                                i,
                                escolha.opcoes().get(i));
            }
            c.commit();
            return buscar(id).orElseThrow();
        } catch (SQLException e) {
            throw Sql.falha(e);
        }
    }

    public Optional<Questionario> buscar(long id) {
        return Sql.listar("SELECT * FROM questionarios WHERE id=?", r -> mapear(r), id).stream()
                .findFirst();
    }

    public List<Questionario> listar(long a) {
        return Sql.listar(
                "SELECT * FROM questionarios WHERE atividade_id=? ORDER BY id", r -> mapear(r), a);
    }

    private Questionario mapear(ResultSet r) throws SQLException {
        long id = r.getLong("id");
        List<Pergunta> perguntas =
                Sql.listar(
                        "SELECT * FROM perguntas WHERE questionario_id=? ORDER BY id",
                        p -> {
                            long pid = p.getLong("id");
                            TipoResposta tipo =
                                    switch (p.getString("tipo")) {
                                        case "TEXTO" -> new Texto();
                                        case "ESCALA" ->
                                                new Escala(p.getInt("minimo"), p.getInt("maximo"));
                                        case "ESCOLHA_UNICA" ->
                                                new EscolhaUnica(
                                                        Sql.listar(
                                                                "SELECT texto FROM opcoes_pergunta"
                                                                    + " WHERE pergunta_id=? ORDER"
                                                                    + " BY ordem",
                                                                o -> o.getString(1),
                                                                pid));
                                        default ->
                                                throw new SQLException(
                                                        "Tipo de pergunta desconhecido.");
                                    };
                            return new Pergunta(pid, p.getString("enunciado"), tipo);
                        },
                        id);
        return new Questionario(id, r.getLong("atividade_id"), r.getString("titulo"), perguntas);
    }

    public void responder(long q, Avaliacao a) {
        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            long id =
                    Sql.executar(
                            c,
                            "INSERT INTO avaliacoes(questionario_id,usuario_id,instante) VALUES"
                                    + " (?,?,?)",
                            q,
                            a.usuarioId(),
                            a.instante().atOffset(ZoneOffset.UTC));
            for (var r : a.respostas().entrySet())
                Sql.executar(
                        c, "INSERT INTO respostas VALUES (?,?,?)", id, r.getKey(), r.getValue());
            c.commit();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                throw new RegraViolada("Você já respondeu este questionário.");
            throw Sql.falha(e);
        }
    }

    public List<Avaliacao> respostas(long q) {
        return Sql.listar(
                "SELECT * FROM avaliacoes WHERE questionario_id=? ORDER BY id",
                r -> {
                    Map<Long, String> valores = new LinkedHashMap<>();
                    Sql.listar(
                            "SELECT * FROM respostas WHERE avaliacao_id=?",
                            v -> {
                                valores.put(v.getLong("pergunta_id"), v.getString("valor"));
                                return 0;
                            },
                            r.getLong("id"));
                    return new Avaliacao(
                            r.getLong("usuario_id"),
                            r.getObject("instante", OffsetDateTime.class).toInstant(),
                            valores);
                },
                q);
    }
}
