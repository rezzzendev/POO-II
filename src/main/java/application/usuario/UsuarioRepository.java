package application.usuario;

import domain.usuario.Usuario;

import java.util.Optional;

/** Porta de saída: o que a aplicação precisa da persistência de usuários. */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(String email);
}
