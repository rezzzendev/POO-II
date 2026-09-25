package adapter.out.persistence;

import application.frequencia.FrequenciaRepository;

import domain.frequencia.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class FrequenciaRepositoryJdbc implements FrequenciaRepository {
    public String politica(long id) {
        return Sql.listar(
                        "SELECT tipo FROM politicas_frequencia WHERE atividade_id=?",
                        r -> r.getString(1),
                        id)
                .stream()
                .findFirst()
                .orElse("CHECK_IN");
    }

    public void configurar(long id, String p) {
        Sql.executar("MERGE INTO politicas_frequencia KEY(atividade_id) VALUES (?,?)", id, p);
        Sql.executar("DELETE FROM codigos_frequencia WHERE atividade_id=?", id);
    }

    public boolean possuiHistorico(long id) {
        return !Sql.listar(
                        "SELECT id FROM registros_frequencia WHERE atividade_id=? FETCH FIRST 1 ROW"
                                + " ONLY",
                        r -> r.getLong(1),
                        id)
                .isEmpty();
    }

    public List<RegistroFrequencia> registros(long a, long u) {
        return Sql.listar(
                "SELECT * FROM registros_frequencia WHERE atividade_id=? AND usuario_id=? ORDER BY"
                        + " id",
                r ->
                        new RegistroFrequencia(
                                r.getLong("id"),
                                a,
                                u,
                                TipoMarcacao.valueOf(r.getString("tipo")),
                                r.getString("origem"),
                                r.getLong("responsavel_id"),
                                r.getObject("instante", OffsetDateTime.class).toInstant(),
                                r.getString("justificativa")),
                a,
                u);
    }

    public void registrar(RegistroFrequencia r) {
        Sql.executar(
                "INSERT INTO"
                    + " registros_frequencia(atividade_id,usuario_id,tipo,origem,responsavel_id,instante,justificativa)"
                    + " VALUES (?,?,?,?,?,?,?)",
                r.atividadeId(),
                r.usuarioId(),
                r.tipo().name(),
                r.origem(),
                r.responsavelId(),
                r.instante().atOffset(ZoneOffset.UTC),
                r.justificativa());
    }

    public void salvarCodigo(CodigoFrequencia c) {
        Sql.executar(
                "INSERT INTO codigos_frequencia VALUES (?,?,?,?)",
                c.token(),
                c.atividadeId(),
                c.tipo().name(),
                c.validade().atOffset(ZoneOffset.UTC));
    }

    public Optional<CodigoFrequencia> codigo(String t) {
        return Sql.listar(
                        "SELECT * FROM codigos_frequencia WHERE token=?",
                        r ->
                                new CodigoFrequencia(
                                        t,
                                        r.getLong("atividade_id"),
                                        TipoMarcacao.valueOf(r.getString("tipo")),
                                        r.getObject("validade", OffsetDateTime.class).toInstant()),
                        t)
                .stream()
                .findFirst();
    }
}
