package domain.frequencia;

import domain.RegraViolada;

import java.util.*;

/**
 * Histórico imutável. Última correção manual prevalece e todas as anteriores permanecem auditáveis.
 */
public class Frequencia {
    private final PoliticaFrequencia politica;
    private final List<RegistroFrequencia> registros;

    public Frequencia(PoliticaFrequencia p, List<RegistroFrequencia> r) {
        politica = p;
        registros = List.copyOf(r);
    }

    public boolean presente() {
        for (int i = registros.size() - 1; i >= 0; i--) {
            TipoMarcacao t = registros.get(i).tipo();
            if (t == TipoMarcacao.VALIDACAO_MANUAL) return true;
            if (t == TipoMarcacao.INVALIDACAO_MANUAL) return false;
        }
        return politica.presente(registros);
    }

    public void validarQr(TipoMarcacao t) {
        if (!politica.aceita(t))
            throw new RegraViolada("Marcação incompatível com a política da atividade.");
        if (registros.stream().anyMatch(r -> r.tipo() == t))
            throw new RegraViolada("Esta marcação já foi registrada.");
        if (t == TipoMarcacao.SAIDA
                && registros.stream().noneMatch(r -> r.tipo() == TipoMarcacao.ENTRADA))
            throw new RegraViolada("Registre a entrada antes da saída.");
    }

    public static PoliticaFrequencia politica(String nome) {
        return switch (nome) {
            case "CHECK_IN" -> new CheckInUnico();
            case "ENTRADA_SAIDA" -> new EntradaSaida();
            case "MANUAL" -> new ValidacaoManual();
            default -> throw new RegraViolada("Política de frequência inválida.");
        };
    }
}
