package domain.frequencia;

import java.util.List;

public class ValidacaoManual implements PoliticaFrequencia {
    public boolean presente(List<RegistroFrequencia> r) {
        return false;
    }

    public boolean aceita(TipoMarcacao t) {
        return false;
    }
}
