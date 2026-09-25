package domain.usuario;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void novoUsuarioAutenticaComSenhaCorreta() {
        Usuario usuario =
                Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE);

        assertTrue(usuario.autenticar("senha123"));
    }

    @Test
    void naoAutenticaComSenhaErrada() {
        Usuario usuario =
                Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE);

        assertFalse(usuario.autenticar("outraSenha"));
    }

    @Test
    void senhaNuncaFicaGuardadaEmTextoPuro() {
        Usuario usuario =
                Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE);

        assertNotEquals("senha123", usuario.getSenhaHash());
    }

    @Test
    void nomeEObrigatorio() {
        assertThrows(
                UsuarioInvalidoException.class,
                () -> Usuario.novo(" ", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE));
    }

    @Test
    void emailPrecisaTerArroba() {
        assertThrows(
                UsuarioInvalidoException.class,
                () ->
                        Usuario.novo(
                                "Matheus", "matheus-sem-arroba", "senha123", Papel.PARTICIPANTE));
    }

    @Test
    void senhaEObrigatoria() {
        assertThrows(
                UsuarioInvalidoException.class,
                () -> Usuario.novo("Matheus", "matheus@exemplo.com", "", Papel.PARTICIPANTE));
    }

    @Test
    void papelEObrigatorio() {
        assertThrows(
                UsuarioInvalidoException.class,
                () -> Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", null));
    }
}
