package application.frequencia;

import domain.frequencia.*;

import java.util.*;

public interface FrequenciaRepository {
    String politica(long atividadeId);

    void configurar(long atividadeId, String politica);

    List<RegistroFrequencia> registros(long atividadeId, long usuarioId);

    boolean possuiHistorico(long atividadeId);

    void registrar(RegistroFrequencia registro);

    void salvarCodigo(CodigoFrequencia codigo);

    Optional<CodigoFrequencia> codigo(String token);
}
