package domain.usuario;

/*
    Entidade Usuario
    Relacionado: UsuarioInvalidoException, Papel
    RF-01/RF-02/RF-03, RN-01, RNF-05
*/
public class Usuario {

    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private Papel papel;

    public Usuario(Long id, String nome, String email, String senhaHash, Papel papel) {
        validar(nome, email, senhaHash, papel);

        this.id = id;
        this.nome = nome;
        this.email = email.strip().toLowerCase(java.util.Locale.ROOT);
        this.senhaHash = senhaHash;
        this.papel = papel;
    }

    public static Usuario novo(String nome, String email, String senha, Papel papel) {
        if (senha == null || senha.isBlank()) {
            throw new UsuarioInvalidoException("Senha é obrigatória.");
        }
        return new Usuario(null, nome, email, SenhaHasher.hash(senha), papel);
    }

    private static void validar(String nome, String email, String senhaHash, Papel papel) {
        if (nome == null || nome.isBlank() || nome.length() > 255) {
            throw new UsuarioInvalidoException("Nome é obrigatório.");
        }
        if (email == null
                || email.length() > 255
                || !email.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new UsuarioInvalidoException("E-mail inválido.");
        }
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new UsuarioInvalidoException("Senha é obrigatória.");
        }
        if (papel == null) {
            throw new UsuarioInvalidoException("Papel do usuário é obrigatório.");
        }
    }

    public boolean autenticar(String senhaDigitada) {
        return senhaDigitada != null && SenhaHasher.confere(senhaDigitada, senhaHash);
    }

    /** RF-03: participante consulta e atualiza os próprios dados básicos. */
    public void editarPerfil(String nome, String email) {
        validar(nome, email, this.senhaHash, this.papel);
        this.nome = nome;
        this.email = email.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public void alterarPapel(Papel papel) {
        if (papel == null) throw new UsuarioInvalidoException("Papel obrigatório.");
        this.papel = papel;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Usuario{"
                + "id="
                + id
                + ", nome='"
                + nome
                + '\''
                + ", email='"
                + email
                + '\''
                + ", papel="
                + papel
                + '}';
    }
}
