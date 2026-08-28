package adapter.out.persistence.evento;

import adapter.out.persistence.ConnectionFactory;
import domain.evento.Evento;
import domain.evento.Modalidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventoRepositoryJdbcTest {

    private final EventoRepositoryJdbc repository = new EventoRepositoryJdbc();

    @BeforeEach
    void limparTabela() throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE eventos");
        }
    }

    @Test
    void salvaEBuscaPorId() {
        Evento evento = Evento.novo("Semana de Tecnologia", "Palestras e oficinas",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 18, 0),
                Modalidade.PRESENCIAL);

        Evento salvo = repository.salvar(evento);

        assertNotNull(salvo.getId());

        Optional<Evento> encontrado = repository.buscarPorId(salvo.getId());
        assertTrue(encontrado.isPresent());
        assertEquals("Semana de Tecnologia", encontrado.get().getTitulo());
        assertEquals(Modalidade.PRESENCIAL, encontrado.get().getModalidade());
    }

    @Test
    void buscaPorIdInexistenteRetornaVazio() {
        assertTrue(repository.buscarPorId(-1L).isEmpty());
    }

    @Test
    void listaTodosOrdenadosPeloInicio() {
        repository.salvar(Evento.novo("Evento B", null,
                LocalDateTime.of(2026, 12, 1, 9, 0),
                LocalDateTime.of(2026, 12, 1, 12, 0),
                Modalidade.HIBRIDO));
        repository.salvar(Evento.novo("Evento A", null,
                LocalDateTime.of(2026, 11, 1, 9, 0),
                LocalDateTime.of(2026, 11, 1, 12, 0),
                Modalidade.ONLINE));

        List<Evento> todos = repository.listarTodos();

        assertEquals(2, todos.size());
        assertEquals("Evento A", todos.get(0).getTitulo());
        assertEquals("Evento B", todos.get(1).getTitulo());
    }
}
