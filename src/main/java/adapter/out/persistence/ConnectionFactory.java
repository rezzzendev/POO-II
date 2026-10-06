package adapter.out.persistence;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/** JDBC puro. Migrações aditivas não apagam a base existente. */
public class ConnectionFactory {
    private static final Set<String> inicializados = new HashSet<>();

    public static synchronized Connection getConnection() throws SQLException {
        String url = System.getProperty("db.url", "jdbc:postgresql://localhost:5432/eventos");
        String usuario = System.getProperty("db.user", "eventos");
        String senha = System.getProperty("db.password", "eventos");
        try {
            Class.forName(
                    url.startsWith("jdbc:h2:")
                            ? "org.h2.Driver"
                            : "org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        Connection c = DriverManager.getConnection(url, usuario, senha);
        if (!inicializados.contains(url)) {
            try {
                executar(c, "/db/001-inicial.sql");
                executar(c, "/db/002-politicas.sql");
                inicializados.add(url);
            } catch (SQLException e) {
                c.close();
                throw e;
            }
        }
        return c;
    }

    private static void executar(Connection c, String recurso) throws SQLException {
        try (InputStream in = ConnectionFactory.class.getResourceAsStream(recurso);
                Statement stmt = c.createStatement()) {
            if (in == null) throw new IOException("Migração ausente: " + recurso);
            for (String sql : new String(in.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                if (!sql.isBlank()) stmt.execute(sql);
            }
        } catch (IOException e) {
            throw new SQLException("Erro ao ler migração", e);
        }
    }
}
