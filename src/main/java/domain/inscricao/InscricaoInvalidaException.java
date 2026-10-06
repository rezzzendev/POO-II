package domain.inscricao;

import domain.RegraViolada;

public class InscricaoInvalidaException extends RegraViolada {

    public InscricaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
