package domain.avaliacao;

import java.time.Instant;
import java.util.Map;

public record Avaliacao(long usuarioId, Instant instante, Map<Long, String> respostas) {
    public Avaliacao {
        respostas = Map.copyOf(respostas);
    }
}
