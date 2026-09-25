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
        String url = System.getProperty("db.url", "jdbc:h2:file:./data/eventos;DB_CLOSE_DELAY=-1");
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        Connection c = DriverManager.getConnection(url, "eventos", "eventos");
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
