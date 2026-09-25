package adapter.out.persistence.inscricao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.atividade.AtividadeRepositoryJdbc;
import adapter.out.persistence.evento.EventoRepositoryJdbc;
import adapter.out.persistence.usuario.UsuarioRepositoryJdbc;

import domain.atividade.Atividade;
import domain.evento.Evento;
import domain.evento.Modalidade;
import domain.inscricao.Inscricao;
import domain.usuario.Papel;
import domain.usuario.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class InscricaoRepositoryJdbcTest {

    private final EventoRepositoryJdbc eventoRepository = new EventoRepositoryJdbc();
    private final UsuarioRepositoryJdbc usuarioRepository = new UsuarioRepositoryJdbc();
    private final AtividadeRepositoryJdbc atividadeRepository =
            new AtividadeRepositoryJdbc(eventoRepository, usuarioRepository);
    private final InscricaoRepositoryJdbc repository =
            new InscricaoRepositoryJdbc(usuarioRepository, eventoRepository);

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

    private Evento eventoPublicado() {
        Evento evento =
                eventoRepository.salvar(
                        Evento.novo(
                                "Semana de Tecnologia",
                                "desc",
                                LocalDateTime.of(2026, 10, 1, 8, 0),
                                LocalDateTime.of(2026, 10, 1, 20, 0),
                                Modalidade.PRESENCIAL));
        evento.publicar();
        return eventoRepository.salvar(evento);
    }

    private Usuario usuarioSalvo() {
        return usuarioRepository.salvar(
                Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE));
    }

    private Atividade atividadeSalva(Evento evento, String local, int horaInicio, int horaFim) {
        return atividadeRepository.salvar(
                Atividade.nova(
                        "Atividade",
                        "d",
                        "PALESTRA",
                        null,
                        local,
                        LocalDateTime.of(2026, 10, 1, horaInicio, 0),
                        LocalDateTime.of(2026, 10, 1, horaFim, 0),
                        null,
                        evento));
    }

    @Test
    void salvaEBuscaComAtividadesEscolhidas() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();
        Atividade a1 = atividadeSalva(evento, "Sala 1", 9, 10);
        Atividade a2 = atividadeSalva(evento, "Sala 2", 11, 12);
        Inscricao inscricao = Inscricao.nova(usuario, evento, List.of(a1.getId(), a2.getId()));

        Inscricao salva = repository.salvar(inscricao);

        assertNotNull(salva.getId());
        Optional<Inscricao> encontrada = repository.buscarPorId(salva.getId());
        assertTrue(encontrada.isPresent());
        assertEquals(2, encontrada.get().getAtividadeIds().size());
    }

    @Test
    void listaPorUsuarioEPorEvento() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();
        repository.salvar(Inscricao.nova(usuario, evento, List.of()));

        assertEquals(1, repository.listarPorUsuario(usuario.getId()).size());
        assertEquals(1, repository.listarPorEvento(evento.getId()).size());
    }

    @Test
    void contaConfirmadosPorAtividade() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();
        Atividade atividade = atividadeSalva(evento, "Sala 1", 9, 10);
        repository.salvar(Inscricao.nova(usuario, evento, List.of(atividade.getId())));

        assertEquals(1, repository.contarConfirmadosPorAtividade(atividade.getId(), null));
        assertEquals(0, repository.contarConfirmadosPorAtividade(999L, null));
    }

    @Test
    void contagemExcluiAPropriaInscricaoQuandoPedido() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();
        Atividade atividade = atividadeSalva(evento, "Sala 1", 9, 10);
        Inscricao salva =
                repository.salvar(Inscricao.nova(usuario, evento, List.of(atividade.getId())));

        assertEquals(1, repository.contarConfirmadosPorAtividade(atividade.getId(), null));
        assertEquals(0, repository.contarConfirmadosPorAtividade(atividade.getId(), salva.getId()));
    }

    @Test
    void cancelarNaoContaMaisParaVagas() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();
        Atividade atividade = atividadeSalva(evento, "Sala 1", 9, 10);
        Inscricao salva =
                repository.salvar(Inscricao.nova(usuario, evento, List.of(atividade.getId())));

        salva.cancelar();
        repository.salvar(salva);

        assertEquals(0, repository.contarConfirmadosPorAtividade(atividade.getId(), null));
    }

    @Test
    void detectaInscricaoAtivaExistente() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();
        repository.salvar(Inscricao.nova(usuario, evento, List.of()));

        assertTrue(repository.existeInscricaoAtiva(usuario.getId(), evento.getId()));
    }

    @Test
    void semInscricaoNaoAcusaAtiva() {
        Evento evento = eventoPublicado();
        Usuario usuario = usuarioSalvo();

        assertFalse(repository.existeInscricaoAtiva(usuario.getId(), evento.getId()));
    }

    @Test
    void falhaNaEscolhaNaoDeixaInscricaoParcial() {
        Evento e = eventoPublicado();
        Usuario u = usuarioSalvo();
        org.junit.jupiter.api.Assertions.assertThrows(
                adapter.out.persistence.PersistenciaException.class,
                () -> repository.salvar(Inscricao.nova(u, e, List.of(-1L))));
        assertTrue(repository.listarPorEvento(e.getId()).isEmpty());
        Atividade a = atividadeSalva(e, "Sala 1", 9, 10);
        Inscricao i = repository.salvar(Inscricao.nova(u, e, List.of(a.getId())));
        i.definirAtividades(List.of(-1L));
        org.junit.jupiter.api.Assertions.assertThrows(
                adapter.out.persistence.PersistenciaException.class, () -> repository.salvar(i));
        assertEquals(
                List.of(a.getId()),
                repository.buscarPorId(i.getId()).orElseThrow().getAtividadeIds());
    }
}
