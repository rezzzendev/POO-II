package domain.frequencia;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

class FrequenciaTest {
    private RegistroFrequencia registro(TipoMarcacao t, int segundos) {
        return new RegistroFrequencia(
                segundos + 1,
                1,
                1,
                t,
                t.name().contains("MANUAL") ? "MANUAL" : "QR",
                2,
                Instant.EPOCH.plusSeconds(segundos),
                "Conferido pelo organizador");
    }

    @Test
    void checkInValidaEDuplicidadeFalha() {
        var f = new Frequencia(new CheckInUnico(), List.of(registro(TipoMarcacao.CHECK_IN, 1)));
        assertTrue(f.presente());
        assertThrows(IllegalArgumentException.class, () -> f.validarQr(TipoMarcacao.CHECK_IN));
    }

    @Test
    void entradaExigeSaidaPosterior() {
        assertFalse(
                new Frequencia(new EntradaSaida(), List.of(registro(TipoMarcacao.ENTRADA, 1)))
                        .presente());
        assertFalse(
                new Frequencia(
                                new EntradaSaida(),
                                List.of(
                                        registro(TipoMarcacao.SAIDA, 0),
                                        registro(TipoMarcacao.ENTRADA, 1)))
                        .presente());
        assertTrue(
                new Frequencia(
                                new EntradaSaida(),
                                List.of(
                                        registro(TipoMarcacao.ENTRADA, 1),
                                        registro(TipoMarcacao.SAIDA, 2)))
                        .presente());
        assertThrows(
                IllegalArgumentException.class,
                () -> new Frequencia(new EntradaSaida(), List.of()).validarQr(TipoMarcacao.SAIDA));
    }

    @Test
    void ultimaCorrecaoPrevaleceSemApagarHistorico() {
        var historico =
                List.of(
                        registro(TipoMarcacao.CHECK_IN, 1),
                        registro(TipoMarcacao.INVALIDACAO_MANUAL, 2),
                        registro(TipoMarcacao.VALIDACAO_MANUAL, 3));
        assertFalse(new Frequencia(new CheckInUnico(), historico.subList(0, 2)).presente());
        assertTrue(new Frequencia(new CheckInUnico(), historico).presente());
        assertEquals(3, historico.size());
    }

    @Test
    void manualNaoAceitaQrEExigeJustificativa() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Frequencia(new ValidacaoManual(), List.of())
                                .validarQr(TipoMarcacao.CHECK_IN));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new RegistroFrequencia(
                                0,
                                1,
                                1,
                                TipoMarcacao.VALIDACAO_MANUAL,
                                "MANUAL",
                                2,
                                Instant.now(),
                                ""));
    }

    @Test
    void validadeExpiraNoInstanteLimite() {
        var c =
                new CodigoFrequencia(
                        "abc", 1, TipoMarcacao.CHECK_IN, Instant.EPOCH.plusSeconds(300));
        assertDoesNotThrow(() -> c.validar(Instant.EPOCH.plusSeconds(299)));
        assertThrows(
                IllegalArgumentException.class, () -> c.validar(Instant.EPOCH.plusSeconds(300)));
    }
}
