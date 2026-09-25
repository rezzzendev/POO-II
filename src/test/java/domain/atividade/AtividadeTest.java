package domain.atividade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import domain.evento.Evento;
import domain.evento.Modalidade;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class AtividadeTest {

    private final Evento evento =
            Evento.novo(
                    "Semana de Tecnologia",
                    "desc",
                    LocalDateTime.of(2026, 10, 1, 8, 0),
                    LocalDateTime.of(2026, 10, 1, 20, 0),
                    Modalidade.PRESENCIAL);

    private Atividade atividade(String local, int horaInicio, int horaFim) {
        return atividade(local, horaInicio, horaFim, null);
    }

    private Atividade atividade(String local, int horaInicio, int horaFim, Integer capacidade) {
        return Atividade.nova(
                "Palestra",
                "desc",
                "PALESTRA",
                "Trilha A",
                local,
                LocalDateTime.of(2026, 10, 1, horaInicio, 0),
                LocalDateTime.of(2026, 10, 1, horaFim, 0),
                capacidade,
                evento);
    }

    @Test
    void tituloEObrigatorio() {
        assertThrows(
                AtividadeInvalidaException.class,
                () ->
                        Atividade.nova(
                                " ",
                                "desc",
                                "PALESTRA",
                                null,
                                "Auditório",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                LocalDateTime.of(2026, 10, 1, 10, 0),
                                null,
                                evento));
    }

    @Test
    void tipoEObrigatorio() {
        assertThrows(
                AtividadeInvalidaException.class,
                () ->
                        Atividade.nova(
                                "Palestra",
                                "desc",
                                " ",
                                null,
                                "Auditório",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                LocalDateTime.of(2026, 10, 1, 10, 0),
                                null,
                                evento));
    }

    @Test
    void localEObrigatorio() {
        assertThrows(
                AtividadeInvalidaException.class,
                () ->
                        Atividade.nova(
                                "Palestra",
                                "desc",
                                "PALESTRA",
                                null,
                                " ",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                LocalDateTime.of(2026, 10, 1, 10, 0),
                                null,
                                evento));
    }

    @Test
    void naoExisteAtividadeSemEvento() {
        assertThrows(
                AtividadeInvalidaException.class,
                () ->
                        Atividade.nova(
                                "Palestra",
                                "desc",
                                "PALESTRA",
                                null,
                                "Auditório",
                                LocalDateTime.of(2026, 10, 1, 9, 0),
                                LocalDateTime.of(2026, 10, 1, 10, 0),
                                null,
                                null));
    }

    @Test
    void capacidadeZeroOuNegativaEInvalida() {
        assertThrows(AtividadeInvalidaException.class, () -> atividade("Auditório", 9, 10, 0));
    }

    @Test
    void mesmoLocalComHorarioSobrepostoConflita() {
        Atividade a = atividade("Auditório", 9, 11);
        Atividade b = atividade("Auditório", 10, 12);

        assertTrue(a.conflitaCom(b));
        assertTrue(a.sobrepoeHorario(b));
    }

    @Test
    void localDiferenteNuncaConflitaMesmoComHorarioIgual() {
        Atividade a = atividade("Auditório", 9, 11);
        Atividade b = atividade("Sala 2", 9, 11);

        assertFalse(a.conflitaCom(b));
        assertTrue(a.sobrepoeHorario(b));
    }

    @Test
    void mesmoLocalBackToBackNaoConflita() {
        Atividade a = atividade("Auditório", 9, 10);
        Atividade b = atividade("Auditório", 10, 11);

        assertFalse(a.conflitaCom(b));
    }

    @Test
    void semCapacidadeVagaSempreDisponivel() {
        Atividade atividade = atividade("Auditório", 9, 10, null);

        assertTrue(atividade.temVagaDisponivel(999));
    }

    @Test
    void comCapacidadeVagaFechaQuandoAtinjeLimite() {
        Atividade atividade = atividade("Auditório", 9, 10, 2);

        assertTrue(atividade.temVagaDisponivel(1));
        assertFalse(atividade.temVagaDisponivel(2));
    }

    @Test
    void editarSoAtualizaCamposPermitidos() {
        Atividade atividade = atividade("Auditório", 9, 11);

        atividade.editar(
                "Nova palestra",
                "nova desc",
                "OFICINA",
                "Trilha B",
                "Sala 3",
                LocalDateTime.of(2026, 10, 1, 14, 0),
                LocalDateTime.of(2026, 10, 1, 16, 0),
                10);

        assertEquals("Nova palestra", atividade.getTitulo());
        assertEquals("Sala 3", atividade.getLocal());
        assertEquals(10, atividade.getCapacidade());
    }
}
