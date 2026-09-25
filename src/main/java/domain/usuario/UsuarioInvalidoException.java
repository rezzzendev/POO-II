package domain.usuario;

public class UsuarioInvalidoException extends RuntimeException {

    public UsuarioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
