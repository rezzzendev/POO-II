package application.evento;

import application.atividade.AtividadeRepository;
import domain.evento.Evento;
import domain.evento.EventoInvalidoException;
import domain.evento.Modalidade;
import domain.evento.StatusEvento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Casos de uso de evento. O handler cuida do HTTP; esta classe cuida das regras. */
public class EventoService {
    private final EventoRepository eventos;
    private final AtividadeRepository atividades;

    public EventoService(EventoRepository eventos, AtividadeRepository atividades) {
        this.eventos = eventos;
        this.atividades = atividades;
    }

    public List<Evento> listar(boolean podeVerTodos) {
        return eventos.listarTodos().stream()
                .filter(e -> podeVerTodos || e.getStatus() == StatusEvento.PUBLICADO)
                .toList();
    }

    public Optional<Evento> buscar(long id, boolean podeVerTodos) {
        return eventos.buscarPorId(id)
                .filter(e -> podeVerTodos || e.getStatus() == StatusEvento.PUBLICADO);
    }

    public Evento criar(
            String titulo,
            String descricao,
            LocalDateTime inicio,
            LocalDateTime fim,
            Modalidade modalidade,
            String local,
            String fuso) {
        Evento evento = Evento.novo(titulo, descricao, inicio, fim, modalidade);
        evento.definirLocalEFuso(local, fuso);
        return eventos.salvar(evento);
    }

    public Evento editar(
            long id,
            String titulo,
            String descricao,
            LocalDateTime inicio,
            LocalDateTime fim,
            Modalidade modalidade,
            String local,
            String fuso) {
        Evento evento = obter(id);
        if (!evento.getFuso().getId().equals(fuso))
            throw new EventoInvalidoException(
                    "O fuso é definido na criação do evento e não pode ser alterado.");

        for (var atividade : atividades.listarPorEvento(id)) {
            if (inicio == null
                    || fim == null
                    || atividade.getInicio().isBefore(inicio)
                    || atividade.getFim().isAfter(fim))
                throw new EventoInvalidoException(
                        "O período precisa abranger as atividades já cadastradas.");
        }

        evento.editar(titulo, descricao, inicio, fim, modalidade);
        evento.definirLocalEFuso(local, fuso);
        return eventos.salvar(evento);
    }

    public Evento publicar(long id) {
        Evento evento = obter(id);
        evento.publicar();
        return eventos.salvar(evento);
    }

    public Evento encerrar(long id) {
        Evento evento = obter(id);
        evento.encerrar();
        return eventos.salvar(evento);
    }

    public void remover(long id) {
        Evento evento = obter(id);
        if (evento.getStatus() != StatusEvento.RASCUNHO)
            throw new EventoInvalidoException("Somente rascunhos podem ser removidos.");
        eventos.remover(id);
    }

    private Evento obter(long id) {
        return eventos.buscarPorId(id)
                .orElseThrow(() -> new NoSuchElementException("Evento " + id + " não encontrado."));
    }
}
