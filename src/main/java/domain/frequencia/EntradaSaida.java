package domain.frequencia;

import java.util.List;

public class EntradaSaida implements PoliticaFrequencia {
    public boolean presente(List<RegistroFrequencia> registros) {
        return registros.stream()
                .filter(r -> r.tipo() == TipoMarcacao.ENTRADA)
                .anyMatch(
                        e ->
                                registros.stream()
                                        .anyMatch(
                                                s ->
                                                        s.tipo() == TipoMarcacao.SAIDA
                                                                && s.instante()
                                                                        .isAfter(e.instante())));
    }

    public boolean aceita(TipoMarcacao t) {
        return t == TipoMarcacao.ENTRADA || t == TipoMarcacao.SAIDA;
    }
}
