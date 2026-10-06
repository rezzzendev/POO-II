package domain.atividade;

import domain.RegraViolada;

public class AtividadeInvalidaException extends RegraViolada {

    public AtividadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}
