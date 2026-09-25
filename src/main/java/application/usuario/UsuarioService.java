package application.usuario;

import domain.usuario.*;

public class UsuarioService {
    private final UsuarioRepository usuarios;

    public UsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario cadastrar(String nome, String email, String senha) {
        Usuario u = Usuario.novo(nome, email, senha, Papel.PARTICIPANTE);
        if (usuarios.buscarPorEmail(u.getEmail()).isPresent())
            throw new UsuarioInvalidoException("Já existe uma conta com esse e-mail.");
        return usuarios.salvar(u);
    }

    public Usuario autenticar(String email, String senha) {
        Usuario u = email == null ? null : usuarios.buscarPorEmail(email).orElse(null);
        return u != null && u.autenticar(senha) ? u : null;
    }

    public Usuario editar(Usuario usuario, String nome, String email) {
        usuario.editarPerfil(nome, email);
        return usuarios.salvar(usuario);
    }
}
