package domain.frequencia;

import domain.RegraViolada;

import java.time.Instant;

public record RegistroFrequencia(
        long id,
        long atividadeId,
        long usuarioId,
        TipoMarcacao tipo,
        String origem,
        long responsavelId,
        Instant instante,
        String justificativa) {
    public RegistroFrequencia {
        if (tipo == null
                || instante == null
                || atividadeId <= 0
                || usuarioId <= 0
                || responsavelId <= 0) throw new RegraViolada("Marcação inválida.");
        if (!"QR".equals(origem) && !"MANUAL".equals(origem))
            throw new RegraViolada("Origem inválida.");
        if (justificativa == null
                || justificativa.length() > 1000
                || (origem.equals("MANUAL") && justificativa.isBlank()))
            throw new RegraViolada("Lançamento manual exige justificativa de até 1000 caracteres.");
    }
}
