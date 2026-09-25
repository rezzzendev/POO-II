package domain.avaliacao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.*;

class QuestionarioTest {
    private final Questionario q =
            new Questionario(
                    1,
                    1,
                    "Avaliação",
                    List.of(
                            new Pergunta(1, "Comentário", new Texto()),
                            new Pergunta(2, "Escolha", new EscolhaUnica(List.of("Sim", "Não"))),
                            new Pergunta(3, "Nota", new Escala(1, 5))));

    @Test
    void validaTresTiposPorPolimorfismo() {
        assertDoesNotThrow(() -> q.validarRespostas(Map.of(1L, "Ótimo", 2L, "Sim", 3L, "5")));
    }

    @Test
    void rejeitaAusenciaExtraOpcaoENotaInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> q.validarRespostas(Map.of(1L, "Ok")));
        assertThrows(
                IllegalArgumentException.class,
                () -> q.validarRespostas(Map.of(1L, "Ok", 2L, "Talvez", 3L, "5")));
        assertThrows(
                IllegalArgumentException.class,
                () -> q.validarRespostas(Map.of(1L, "Ok", 2L, "Sim", 3L, "6")));
        assertThrows(
                IllegalArgumentException.class,
                () -> q.validarRespostas(Map.of(1L, " ", 2L, "Sim", 3L, "5")));
    }

    @Test
    void configuracaoProtegidaEImutavel() {
        assertThrows(IllegalArgumentException.class, () -> new Escala(5, 1));
        assertThrows(IllegalArgumentException.class, () -> new EscolhaUnica(List.of("Sim", "Sim")));
        assertThrows(UnsupportedOperationException.class, () -> q.perguntas().clear());
    }
}
