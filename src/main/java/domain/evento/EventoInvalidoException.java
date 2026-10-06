package domain.evento;

import domain.RegraViolada;

public class EventoInvalidoException extends RegraViolada {

    public EventoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
