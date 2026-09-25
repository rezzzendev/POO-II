package domain.inscricao;

import domain.evento.Evento;
import domain.evento.StatusEvento;
import domain.usuario.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
    Entidade Inscricao
    Relacionado: Usuario, Evento, StatusInscricao, InscricaoInvalidaException
    RF-10 a RF-18, RN-04, RN-05, RN-06, RN-15

    Simplificação declarada: sem fluxo de aprovação (não existe PENDENTE) —
    toda inscrição nasce CONFIRMADA. O requisito não pede aprovação manual;
    "lista de espera" é item opcional (P), fora do núcleo obrigatório.
*/
public class Inscricao {

    private Long id;
    private Usuario usuario;
    private Evento evento;
    private List<Long> atividadeIds;
    private StatusInscricao status;
    private LocalDateTime dataInscricao;

    public Inscricao(
            Long id,
            Usuario usuario,
            Evento evento,
            List<Long> atividadeIds,
            StatusInscricao status,
            LocalDateTime dataInscricao) {
        validar(usuario, evento, status, dataInscricao);

        this.id = id;
        this.usuario = usuario;
        this.evento = evento;
        if (atividadeIds != null
                && (atividadeIds.stream().anyMatch(java.util.Objects::isNull)
                        || atividadeIds.stream().distinct().count() != atividadeIds.size()))
            throw new InscricaoInvalidaException(
                    "Seleção contém atividades inválidas ou repetidas.");
        this.atividadeIds =
                atividadeIds == null ? new ArrayList<>() : new ArrayList<>(atividadeIds);
        this.status = status;
        this.dataInscricao = dataInscricao;
    }

    /** RN-04: o estado do evento controla a abertura de inscrições. */
    public static Inscricao nova(Usuario usuario, Evento evento, List<Long> atividadeIds) {
        if (evento != null && evento.getStatus() != StatusEvento.PUBLICADO) {
            throw new InscricaoInvalidaException(
                    "Só é possível se inscrever em um evento publicado.");
        }
        return new Inscricao(
                null,
                usuario,
                evento,
                atividadeIds,
                StatusInscricao.CONFIRMADA,
                LocalDateTime.now());
    }

    private static void validar(
            Usuario usuario, Evento evento, StatusInscricao status, LocalDateTime dataInscricao) {
        if (usuario == null) {
            throw new InscricaoInvalidaException("Inscrição precisa de um usuário.");
        }
        if (evento == null) {
            throw new InscricaoInvalidaException("Inscrição precisa de um evento.");
        }
        if (status == null) {
            throw new InscricaoInvalidaException("Status da inscrição é obrigatório.");
        }
        if (dataInscricao == null) {
            throw new InscricaoInvalidaException("Data da inscrição é obrigatória.");
        }
    }

    /** RF-16: participante escolhe (ou troca) as atividades de interesse. */
    public void definirAtividades(List<Long> atividadeIds) {
        if (status != StatusInscricao.CONFIRMADA)
            throw new InscricaoInvalidaException("Inscrição cancelada não pode ser alterada.");
        if (atividadeIds != null
                && (atividadeIds.stream().anyMatch(java.util.Objects::isNull)
                        || atividadeIds.stream().distinct().count() != atividadeIds.size()))
            throw new InscricaoInvalidaException(
                    "Seleção contém atividades inválidas ou repetidas.");
        this.atividadeIds =
                atividadeIds == null ? new ArrayList<>() : new ArrayList<>(atividadeIds);
    }

    /** RF-15/RN-15: cancelamento libera a vaga (a contagem só soma CONFIRMADA). */
    public void cancelar() {
        if (status == StatusInscricao.CANCELADA) {
            throw new InscricaoInvalidaException("Inscrição já está cancelada.");
        }
        this.status = StatusInscricao.CANCELADA;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Evento getEvento() {
        return evento;
    }

    public List<Long> getAtividadeIds() {
        return Collections.unmodifiableList(atividadeIds);
    }

    public StatusInscricao getStatus() {
        return status;
    }

    public LocalDateTime getDataInscricao() {
        return dataInscricao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inscricao)) return false;
        Inscricao outra = (Inscricao) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Inscricao{"
                + "id="
                + id
                + ", usuario="
                + (usuario == null ? null : usuario.getId())
                + ", evento="
                + (evento == null ? null : evento.getId())
                + ", status="
                + status
                + '}';
    }
}
