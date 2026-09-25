package domain.atividade;

public class AtividadeInvalidaException extends RuntimeException {

    public AtividadeInvalidaException(String mensagem) {
        super(mensagem);
    }
}
