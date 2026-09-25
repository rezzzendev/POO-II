package domain.frequencia;

import domain.RegraViolada;

import java.time.Instant;

public record CodigoFrequencia(
        String token, long atividadeId, TipoMarcacao tipo, Instant validade) {
    public void validar(Instant agora) {
        if (!agora.isBefore(validade)) throw new RegraViolada("QR Code expirado.");
    }
}
