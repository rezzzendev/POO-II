import adapter.out.persistence.evento.EventoRepositoryJdbc;
import application.evento.EventoRepository;
import domain.evento.Evento;
import domain.evento.Modalidade;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Só um teste manual da cadeia domain -> application -> JDBC -> H2, enquanto
 * o adapter.in.api (que vai virar o Main de verdade) ainda não existe.
 */
public class Main {

    public static void main(String[] args) {
        EventoRepository repository = new EventoRepositoryJdbc();

        Evento evento = Evento.novo(
                "Semana de Tecnologia",
                "Palestras e oficinas sobre POO",
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 18, 0),
                Modalidade.PRESENCIAL
        );
        repository.salvar(evento);

        List<Evento> eventos = repository.listarTodos();
        System.out.println("Eventos cadastrados:");
        eventos.forEach(System.out::println);
    }
}
