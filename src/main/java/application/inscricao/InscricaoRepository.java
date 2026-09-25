package application.inscricao;

import domain.inscricao.Inscricao;

import java.util.List;
import java.util.Optional;

/** Porta de saída: o que a aplicação precisa da persistência de inscrições. */
public interface InscricaoRepository {

    Inscricao salvar(Inscricao inscricao);

    Optional<Inscricao> buscarPorId(Long id);

    List<Inscricao> listarPorUsuario(Long usuarioId);

    List<Inscricao> listarPorEvento(Long eventoId);

    /**
     * RF-14: vagas controladas por atividade. {@code inscricaoIdExcluida} (opcional) tira a própria
     * inscrição da contagem — sem isso, alguém que já tem a vaga fica travado ao só editar a lista
     * de atividades.
     */
    int contarConfirmadosPorAtividade(Long atividadeId, Long inscricaoIdExcluida);

    /** RNF-11: evita duplicidade por repetição acidental. */
    boolean existeInscricaoAtiva(Long usuarioId, Long eventoId);
}
