package adapter.out.persistence.atividade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.evento.EventoRepositoryJdbc;
import adapter.out.persistence.usuario.UsuarioRepositoryJdbc;

import domain.atividade.Atividade;
import domain.atividade.VinculoPessoa;
import domain.evento.Evento;
import domain.evento.Modalidade;
import domain.usuario.Papel;
import domain.usuario.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class AtividadeRepositoryJdbcTest {

    private final EventoRepositoryJdbc eventoRepository = new EventoRepositoryJdbc();
    private final UsuarioRepositoryJdbc usuarioRepository = new UsuarioRepositoryJdbc();
    private final AtividadeRepositoryJdbc repository =
            new AtividadeRepositoryJdbc(eventoRepository, usuarioRepository);

    @BeforeEach
    void limparTabelas() throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM inscricao_atividades");
            stmt.execute("DELETE FROM inscricoes");
            stmt.execute("DELETE FROM atividade_pessoas");
            stmt.execute("DELETE FROM atividades");
            stmt.execute("DELETE FROM eventos");
            stmt.execute("DELETE FROM usuarios");
        }
    }

    private Evento eventoSalvo() {
        return eventoRepository.salvar(
                Evento.novo(
                        "Semana de Tecnologia",
                        "desc",
                        LocalDateTime.of(2026, 10, 1, 8, 0),
                        LocalDateTime.of(2026, 10, 1, 20, 0),
                        Modalidade.PRESENCIAL));
    }

    @Test
    void salvaEBuscaPorId() {
        Evento evento = eventoSalvo();
        Atividade atividade =
                Atividade.nova(
                        "Palestra de abertura",
                        "desc",
                        "PALESTRA",
                        "Trilha A",
                        "Auditório",
                        LocalDateTime.of(2026, 10, 1, 9, 0),
                        LocalDateTime.of(2026, 10, 1, 10, 0),
                        50,
                        evento);

        Atividade salva = repository.salvar(atividade);

        assertNotNull(salva.getId());
        Optional<Atividade> encontrada = repository.buscarPorId(salva.getId());
        assertTrue(encontrada.isPresent());
        assertEquals("Palestra de abertura", encontrada.get().getTitulo());
        assertEquals(50, encontrada.get().getCapacidade());
        assertEquals(evento.getId(), encontrada.get().getEvento().getId());
    }

    @Test
    void listaSoAsAtividadesDoEventoPedido() {
        Evento eventoA = eventoSalvo();
        Evento eventoB =
                eventoRepository.salvar(
                        Evento.novo(
                                "Outro evento",
                                "desc",
                                LocalDateTime.of(2026, 11, 1, 8, 0),
                                LocalDateTime.of(2026, 11, 1, 20, 0),
                                Modalidade.ONLINE));

        repository.salvar(
                Atividade.nova(
                        "Atividade do A",
                        "d",
                        "PALESTRA",
                        null,
                        "Sala 1",
                        LocalDateTime.of(2026, 10, 1, 9, 0),
                        LocalDateTime.of(2026, 10, 1, 10, 0),
                        null,
                        eventoA));
        repository.salvar(
                Atividade.nova(
                        "Atividade do B",
                        "d",
                        "OFICINA",
                        null,
                        "Sala 2",
                        LocalDateTime.of(2026, 11, 1, 9, 0),
                        LocalDateTime.of(2026, 11, 1, 10, 0),
                        null,
                        eventoB));

        List<Atividade> doEventoA = repository.listarPorEvento(eventoA.getId());

        assertEquals(1, doEventoA.size());
        assertEquals("Atividade do A", doEventoA.get(0).getTitulo());
    }

    @Test
    void buscaComFiltrosCombinaveis() {
        Evento evento = eventoSalvo();
        repository.salvar(
                Atividade.nova(
                        "Palestra X",
                        "d",
                        "PALESTRA",
                        "Trilha A",
                        "Auditório",
                        LocalDateTime.of(2026, 10, 1, 9, 0),
                        LocalDateTime.of(2026, 10, 1, 10, 0),
                        null,
                        evento));
        repository.salvar(
                Atividade.nova(
                        "Oficina Y",
                        "d",
                        "OFICINA",
                        "Trilha B",
                        "Sala 2",
                        LocalDateTime.of(2026, 10, 1, 11, 0),
                        LocalDateTime.of(2026, 10, 1, 12, 0),
                        null,
                        evento));

        List<Atividade> porTipo = repository.buscar(null, null, null, "OFICINA", null);
        assertEquals(1, porTipo.size());
        assertEquals("Oficina Y", porTipo.get(0).getTitulo());

        List<Atividade> porData =
                repository.buscar(null, LocalDate.of(2026, 10, 1), null, null, null);
        assertEquals(2, porData.size());

        List<Atividade> porLocalETrilha =
                repository.buscar(null, null, "Trilha A", null, "Auditório");
        assertEquals(1, porLocalETrilha.size());
    }

    @Test
    void removeAtividade() {
        Evento evento = eventoSalvo();
        Atividade salva =
                repository.salvar(
                        Atividade.nova(
                                "Palestra",
                                "d",
                                "PALESTRA",
                                null,
                                "Auditório",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                LocalDateTime.of(2026, 10, 1, 10, 0),
                                null,
                                evento));

        repository.remover(salva.getId());

        assertTrue(repository.buscarPorId(salva.getId()).isEmpty());
    }

    @Test
    void vinculaEListaPessoas() {
        Evento evento = eventoSalvo();
        Atividade atividade =
                repository.salvar(
                        Atividade.nova(
                                "Palestra",
                                "d",
                                "PALESTRA",
                                null,
                                "Auditório",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                LocalDateTime.of(2026, 10, 1, 10, 0),
                                null,
                                evento));
        Usuario palestrante =
                usuarioRepository.salvar(
                        Usuario.novo(
                                "Fulano", "fulano@exemplo.com", "senha123", Papel.PARTICIPANTE));

        repository.vincularPessoa(
                atividade.getId(), new VinculoPessoa(palestrante.getId(), "Fulano", "PALESTRANTE"));

        List<VinculoPessoa> pessoas = repository.listarPessoas(atividade.getId());
        assertEquals(1, pessoas.size());
        assertEquals("PALESTRANTE", pessoas.get(0).getPapel());
    }
}
