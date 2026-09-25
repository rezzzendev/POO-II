package application.avaliacao;

import domain.avaliacao.*;

import java.util.*;

public interface AvaliacaoRepository {
    Questionario salvar(Questionario q);

    Optional<Questionario> buscar(long id);

    List<Questionario> listar(long atividadeId);

    void responder(long questionarioId, Avaliacao avaliacao);

    List<Avaliacao> respostas(long questionarioId);
}
