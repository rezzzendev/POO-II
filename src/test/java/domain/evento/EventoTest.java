package domain.evento;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoTest {

    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 1, 9, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 10, 1, 18, 0);

    @Test
    void novoEventoNasceEmRascunho() {
        Evento evento = Evento.novo("Semana de Tecnologia", "desc", inicio, fim, Modalidade.PRESENCIAL);

        assertEquals(StatusEvento.RASCUNHO, evento.getStatus());
        assertNull(evento.getId());
    }

    @Test
    void tituloEmBrancoNaoPodeSerCriado() {
        assertThrows(EventoInvalidoException.class,
                () -> Evento.novo("  ", "desc", inicio, fim, Modalidade.PRESENCIAL));
    }

    @Test
    void modalidadeEObrigatoria() {
        assertThrows(EventoInvalidoException.class,
                () -> Evento.novo("Título", "desc", inicio, fim, null));
    }

    @Test
    void fimTemQueSerDepoisDoInicio() {
        assertThrows(EventoInvalidoException.class,
                () -> Evento.novo("Título", "desc", fim, inicio, Modalidade.PRESENCIAL));
    }

    @Test
    void publicarSaiDeRascunhoParaPublicado() {
        Evento evento = Evento.novo("Título", "desc", inicio, fim, Modalidade.PRESENCIAL);

        evento.publicar();

        assertEquals(StatusEvento.PUBLICADO, evento.getStatus());
    }

    @Test
    void naoPodePublicarEventoJaPublicado() {
        Evento evento = Evento.novo("Título", "desc", inicio, fim, Modalidade.PRESENCIAL);
        evento.publicar();

        assertThrows(EventoInvalidoException.class, evento::publicar);
    }

    @Test
    void soEncerraEventoQueJaFoiPublicado() {
        Evento evento = Evento.novo("Título", "desc", inicio, fim, Modalidade.PRESENCIAL);

        assertThrows(EventoInvalidoException.class, evento::encerrar);

        evento.publicar();
        evento.encerrar();

        assertEquals(StatusEvento.ENCERRADO, evento.getStatus());
    }

    @Test
    void editarSoFuncionaEnquantoEstiverEmRascunho() {
        Evento evento = Evento.novo("Título", "desc", inicio, fim, Modalidade.PRESENCIAL);

        evento.editar("Novo título", "nova desc", inicio, fim, Modalidade.ONLINE);
        assertEquals("Novo título", evento.getTitulo());

        evento.publicar();
        assertThrows(EventoInvalidoException.class,
                () -> evento.editar("Outro título", "x", inicio, fim, Modalidade.ONLINE));
    }

    @Test
    void doisEventosComMesmoIdSaoIguais() {
        Evento a = new Evento(1L, "A", "desc", inicio, fim, Modalidade.PRESENCIAL, StatusEvento.RASCUNHO);
        Evento b = new Evento(1L, "B", "outra desc", inicio, fim, Modalidade.ONLINE, StatusEvento.PUBLICADO);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
