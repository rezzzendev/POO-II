package domain.inscricao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import domain.evento.Evento;
import domain.evento.Modalidade;
import domain.usuario.Papel;
import domain.usuario.Usuario;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class InscricaoTest {

    private final Usuario usuario =
            Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE);

    private Evento eventoPublicado() {
        Evento evento =
                Evento.novo(
                        "Semana de Tecnologia",
                        "desc",
                        LocalDateTime.of(2026, 10, 1, 8, 0),
                        LocalDateTime.of(2026, 10, 1, 20, 0),
                        Modalidade.PRESENCIAL);
        evento.publicar();
        return evento;
    }

    @Test
    void naoInscreveEmEventoAindaEmRascunho() {
        Evento evento =
                Evento.novo(
                        "Semana de Tecnologia",
                        "desc",
                        LocalDateTime.of(2026, 10, 1, 8, 0),
                        LocalDateTime.of(2026, 10, 1, 20, 0),
                        Modalidade.PRESENCIAL);

        assertThrows(
                InscricaoInvalidaException.class, () -> Inscricao.nova(usuario, evento, List.of()));
    }

    @Test
    void inscricaoNasceConfirmada() {
        Inscricao inscricao = Inscricao.nova(usuario, eventoPublicado(), List.of());

        assertEquals(StatusInscricao.CONFIRMADA, inscricao.getStatus());
    }

    @Test
    void cancelarMudaStatus() {
        Inscricao inscricao = Inscricao.nova(usuario, eventoPublicado(), List.of());

        inscricao.cancelar();

        assertEquals(StatusInscricao.CANCELADA, inscricao.getStatus());
    }

    @Test
    void naoCancelaDuasVezes() {
        Inscricao inscricao = Inscricao.nova(usuario, eventoPublicado(), List.of());
        inscricao.cancelar();

        assertThrows(InscricaoInvalidaException.class, inscricao::cancelar);
    }

    @Test
    void definirAtividadesSubstituiEscolha() {
        Inscricao inscricao = Inscricao.nova(usuario, eventoPublicado(), List.of(1L, 2L));

        inscricao.definirAtividades(List.of(3L));

        assertEquals(List.of(3L), inscricao.getAtividadeIds());
    }

    @Test
    void semUsuarioNaoValida() {
        assertThrows(
                InscricaoInvalidaException.class,
                () -> Inscricao.nova(null, eventoPublicado(), List.of()));
    }

    @Test
    void listaDeAtividadesEImutavelParaQuemLe() {
        Inscricao inscricao = Inscricao.nova(usuario, eventoPublicado(), List.of(1L));

        assertThrows(
                UnsupportedOperationException.class, () -> inscricao.getAtividadeIds().add(2L));
        assertTrue(inscricao.getAtividadeIds().contains(1L));
    }
}
