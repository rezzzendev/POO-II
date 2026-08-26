package application;

import domain.Evento;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída: o que a aplicação precisa da persistência de eventos,
 * sem saber se por trás tem JDBC, Postgres ou qualquer outra coisa.
 */
public interface EventoRepository {

    Evento salvar(Evento evento);

    Optional<Evento> buscarPorId(Long id);

    List<Evento> listarTodos();
}
