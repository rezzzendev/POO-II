package application.atividade;

import application.evento.EventoRepository;
import application.usuario.UsuarioRepository;
import domain.atividade.Atividade;
import domain.atividade.AtividadeInvalidaException;
import domain.atividade.VinculoPessoa;
import domain.evento.Evento;
import domain.evento.StatusEvento;
import domain.usuario.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Casos de uso de atividade, incluindo programação e pessoas vinculadas. */
public class AtividadeService {
    private final AtividadeRepository atividades;
    private final EventoRepository eventos;
    private final UsuarioRepository usuarios;

    public AtividadeService(
            AtividadeRepository atividades,
            EventoRepository eventos,
            UsuarioRepository usuarios) {
        this.atividades = atividades;
        this.eventos = eventos;
        this.usuarios = usuarios;
    }

    public List<Atividade> listar(
            Long eventoId,
            LocalDate data,
            String trilha,
            String tipo,
            String local,
            boolean podeVerTodas) {
        return atividades.buscar(eventoId, data, trilha, tipo, local).stream()
                .filter(a -> podeVerTodas || a.getEvento().getStatus() == StatusEvento.PUBLICADO)
                .toList();
    }

    public Optional<Atividade> buscar(long id, boolean podeVerTodas) {
        return atividades.buscarPorId(id)
                .filter(a -> podeVerTodas || a.getEvento().getStatus() == StatusEvento.PUBLICADO);
    }

    public Atividade criar(
            long eventoId,
            String titulo,
            String descricao,
            String tipo,
            String trilha,
            String local,
            LocalDateTime inicio,
            LocalDateTime fim,
            Integer capacidade) {
        Atividade atividade =
                Atividade.nova(
                        titulo,
                        descricao,
                        tipo,
                        trilha,
                        local,
                        inicio,
                        fim,
                        capacidade,
                        obterEvento(eventoId));
        return salvarValidandoProgramacao(atividade);
    }

    public Atividade editar(
            long id,
            String titulo,
            String descricao,
            String tipo,
            String trilha,
            String local,
            LocalDateTime inicio,
            LocalDateTime fim,
            Integer capacidade) {
        Atividade atividade = obter(id);
        atividade.editar(titulo, descricao, tipo, trilha, local, inicio, fim, capacidade);
        return salvarValidandoProgramacao(atividade);
    }

    public void remover(long id) {
        Atividade atividade = obter(id);
        exigirRascunho(atividade);
        atividades.remover(id);
    }

    public List<VinculoPessoa> listarPessoas(long atividadeId, boolean podeVerTodas) {
        buscar(atividadeId, podeVerTodas)
                .orElseThrow(() -> new NoSuchElementException("Atividade não encontrada."));
        return atividades.listarPessoas(atividadeId);
    }

    public VinculoPessoa vincularPessoa(long atividadeId, long usuarioId, String papel) {
        obter(atividadeId);
        Usuario usuario =
                usuarios.buscarPorId(usuarioId)
                        .orElseThrow(
                                () -> new AtividadeInvalidaException("Usuário não encontrado."));
        return salvarVinculo(atividadeId, usuario, papel);
    }

    public VinculoPessoa vincularPessoa(long atividadeId, String email, String papel) {
        obter(atividadeId);
        if (email == null || email.isBlank())
            throw new AtividadeInvalidaException("E-mail da conta é obrigatório.");
        Usuario usuario =
                usuarios.buscarPorEmail(email)
                        .orElseThrow(
                                () -> new AtividadeInvalidaException(
                                        "Nenhuma conta encontrada com esse e-mail."));
        return salvarVinculo(atividadeId, usuario, papel);
    }

    private VinculoPessoa salvarVinculo(long atividadeId, Usuario usuario, String papel) {
        VinculoPessoa vinculo = new VinculoPessoa(usuario.getId(), usuario.getNome(), papel);
        atividades.vincularPessoa(atividadeId, vinculo);
        return vinculo;
    }

    private Atividade salvarValidandoProgramacao(Atividade candidata) {
        exigirRascunho(candidata);
        if (candidata.getInicio().isBefore(candidata.getEvento().getInicio())
                || candidata.getFim().isAfter(candidata.getEvento().getFim()))
            throw new AtividadeInvalidaException(
                    "Atividade deve ocorrer dentro do período do evento.");

        for (Atividade existente : atividades.listarPorEvento(candidata.getEvento().getId())) {
            if (!existente.getId().equals(candidata.getId()) && candidata.conflitaCom(existente))
                throw new AtividadeInvalidaException(
                        "Conflito de horário/local com " + existente.getTitulo() + ".");
        }
        return atividades.salvar(candidata);
    }

    private Atividade obter(long id) {
        return atividades.buscarPorId(id)
                .orElseThrow(
                        () -> new NoSuchElementException("Atividade " + id + " não encontrada."));
    }

    private Evento obterEvento(long id) {
        return eventos.buscarPorId(id)
                .orElseThrow(() -> new AtividadeInvalidaException("Evento " + id + " não encontrado."));
    }

    private void exigirRascunho(Atividade atividade) {
        if (atividade.getEvento().getStatus() != StatusEvento.RASCUNHO)
            throw new AtividadeInvalidaException(
                    "Programação bloqueada após publicação para preservar inscrições e histórico.");
    }
}
