package domain.inscricao;

public class InscricaoInvalidaException extends RuntimeException {

    public InscricaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
