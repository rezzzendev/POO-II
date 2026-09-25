package domain.frequencia;

import java.util.List;

public class CheckInUnico implements PoliticaFrequencia {
    public boolean presente(List<RegistroFrequencia> r) {
        return r.stream().anyMatch(x -> x.tipo() == TipoMarcacao.CHECK_IN);
    }

    public boolean aceita(TipoMarcacao t) {
        return t == TipoMarcacao.CHECK_IN;
    }
}
