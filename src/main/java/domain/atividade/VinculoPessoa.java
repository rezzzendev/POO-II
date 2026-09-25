package domain.atividade;

import java.util.Objects;

/**
 * RF-08: vincular pessoas às atividades com papéis (palestrante, apresentador, responsável). Objeto
 * de VALOR (ROO-03) — não tem id próprio, igualdade é por conteúdo, é imutável.
 */
public final class VinculoPessoa {

    private final Long usuarioId;
    private final String nomePessoa;
    private final String papel;

    public VinculoPessoa(Long usuarioId, String nomePessoa, String papel) {
        if (usuarioId == null) {
            throw new AtividadeInvalidaException("Pessoa vinculada precisa de um usuário.");
        }
        if (papel == null || papel.isBlank()) {
            throw new AtividadeInvalidaException(
                    "Papel da pessoa na atividade é obrigatório (ex.: PALESTRANTE).");
        }
        this.usuarioId = usuarioId;
        this.nomePessoa = nomePessoa;
        this.papel = papel;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getNomePessoa() {
        return nomePessoa;
    }

    public String getPapel() {
        return papel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VinculoPessoa)) return false;
        VinculoPessoa outro = (VinculoPessoa) o;
        return usuarioId.equals(outro.usuarioId) && papel.equals(outro.papel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, papel);
    }

    @Override
    public String toString() {
        return nomePessoa + " (" + papel + ")";
    }
}
