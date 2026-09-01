package adapter.out.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * H2 embarcado em arquivo local (./data/eventos.mv.db) — RNF-04/RNF-15:
 * banco relacional real, sem exigir servidor instalado.
 */
public class ConnectionFactory {

    private static final String URL = "jdbc:h2:file:./data/eventos";
    private static final String USER = "eventos";
    private static final String PASSWORD = "eventos";

    static {
        // mvn exec:java roda a app numa thread cujo context classloader o
        // DriverManager não enxerga, então o registro automático do driver
        // via ServiceLoader falha. Forçar o carregamento da classe aqui
        // aciona o static block do próprio driver, que se registra sozinho.
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        criarEsquemaSeNecessario(connection);
        return connection;
    }

    private static void criarEsquemaSeNecessario(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS eventos (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    titulo VARCHAR(255) NOT NULL,
                    descricao VARCHAR(2000),
                    inicio TIMESTAMP NOT NULL,
                    fim TIMESTAMP NOT NULL,
                    modalidade VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
                """);
        }
    }
}
