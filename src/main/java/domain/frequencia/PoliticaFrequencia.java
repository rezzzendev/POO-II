package domain.frequencia;

import java.util.List;

/** Strategy: cada atividade compõe a política escolhida. */
public interface PoliticaFrequencia {
    boolean presente(List<RegistroFrequencia> registros);

    boolean aceita(TipoMarcacao tipo);
}
