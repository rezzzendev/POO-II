package adapter.out.persistence;

import java.sql.*;
import java.util.*;

/** Utilitário JDBC pequeno: recursos fechados e parâmetros preparados. Não contém regras. */
public final class Sql {
    private Sql() {}

    @FunctionalInterface
    public interface Leitor<T> {
        T ler(ResultSet r) throws SQLException;
    }

    public static <T> List<T> listar(String sql, Leitor<T> leitor, Object... args) {
        try (Connection c = ConnectionFactory.getConnection()) {
            return listar(c, sql, leitor, args);
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public static <T> List<T> listar(Connection c, String sql, Leitor<T> leitor, Object... args)
            throws SQLException {
        try (PreparedStatement p = preparar(c, sql, args);
                ResultSet r = p.executeQuery()) {
            List<T> lista = new ArrayList<>();
            while (r.next()) lista.add(leitor.ler(r));
            return lista;
        }
    }

    public static void executar(String sql, Object... args) {
        try (Connection c = ConnectionFactory.getConnection()) {
            executar(c, sql, args);
        } catch (SQLException e) {
            throw falha(e);
        }
    }

    public static long executar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = preparar(c, sql, args)) {
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                return r.next() && r.getObject(1) instanceof Number n ? n.longValue() : 0;
            }
        }
    }

    private static PreparedStatement preparar(Connection c, String sql, Object... args)
            throws SQLException {
        PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
        return p;
    }

    public static PersistenciaException falha(SQLException e) {
        return new PersistenciaException("Falha de persistência.", e);
    }
}
