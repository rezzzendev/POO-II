package domain.usuario;

import domain.RegraViolada;

public class UsuarioInvalidoException extends RegraViolada {

    public UsuarioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
