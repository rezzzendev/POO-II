package domain.evento;

public class EventoInvalidoException extends RuntimeException{

    public EventoInvalidoException(String mensagem){
        super(mensagem);
    }
}
