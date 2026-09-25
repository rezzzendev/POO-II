package application.atividade;

import domain.atividade.Atividade;
import domain.atividade.VinculoPessoa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Porta de saída: o que a aplicação precisa da persistência de atividades. */
public interface AtividadeRepository {

    Atividade salvar(Atividade atividade);

    Optional<Atividade> buscarPorId(Long id);

    /** RF-09: filtros combináveis — qualquer parâmetro nulo é ignorado. */
    List<Atividade> buscar(Long eventoId, LocalDate data, String trilha, String tipo, String local);

    default List<Atividade> listarPorEvento(Long eventoId) {
        return buscar(eventoId, null, null, null, null);
    }

    void remover(Long id);

    /** RF-08: vincular pessoas às atividades com papéis. */
    void vincularPessoa(Long atividadeId, VinculoPessoa vinculo);

    List<VinculoPessoa> listarPessoas(Long atividadeId);
}
