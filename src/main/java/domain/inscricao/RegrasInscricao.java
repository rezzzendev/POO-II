package domain.inscricao;

import domain.RegraViolada;

import java.time.*;
import java.util.List;

/** Objeto de valor: configuração imutável do evento. */
public record RegrasInscricao(
        boolean escolherAtividades, boolean controlarVagas, LocalDateTime prazoCancelamento) {
    public RegrasInscricao {
        if (prazoCancelamento == null) throw new RegraViolada("Informe o prazo de cancelamento.");
    }

    public void validarEscolha(List<Long> ids) {
        if (!escolherAtividades && !ids.isEmpty())
            throw new RegraViolada("Este evento aceita inscrição somente no evento.");
        if (ids.stream().distinct().count() != ids.size())
            throw new RegraViolada("Atividade repetida na seleção.");
    }

    public void validarCancelamento(LocalDateTime agora) {
        if (agora.isAfter(prazoCancelamento))
            throw new RegraViolada("Prazo de cancelamento encerrado.");
    }
}
