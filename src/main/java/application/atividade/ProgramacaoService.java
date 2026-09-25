package application.atividade;

import domain.atividade.*;
import domain.evento.StatusEvento;

public class ProgramacaoService {
    private final AtividadeRepository repo;

    public ProgramacaoService(AtividadeRepository repo) {
        this.repo = repo;
    }

    public Atividade salvar(Atividade candidata) {
        exigirRascunho(candidata);
        if (candidata.getInicio().isBefore(candidata.getEvento().getInicio())
                || candidata.getFim().isAfter(candidata.getEvento().getFim()))
            throw new AtividadeInvalidaException(
                    "Atividade deve ocorrer dentro do período do evento.");
        for (Atividade existente : repo.listarPorEvento(candidata.getEvento().getId())) {
            if (!existente.getId().equals(candidata.getId()) && candidata.conflitaCom(existente))
                throw new AtividadeInvalidaException(
                        "Conflito de horário/local com " + existente.getTitulo() + ".");
        }
        return repo.salvar(candidata);
    }

    public void remover(long id) {
        Atividade a =
                repo.buscarPorId(id)
                        .orElseThrow(
                                () -> new AtividadeInvalidaException("Atividade não encontrada."));
        exigirRascunho(a);
        repo.remover(id);
    }

    private void exigirRascunho(Atividade a) {
        if (a.getEvento().getStatus() != StatusEvento.RASCUNHO)
            throw new AtividadeInvalidaException(
                    "Programação bloqueada após publicação para preservar inscrições e histórico.");
    }
}
