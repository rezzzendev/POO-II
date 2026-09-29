package domain.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void emailPrecisaTerDominioCompleto() {
        var erro =
                assertThrows(
                        UsuarioInvalidoException.class,
                        () ->
                                Usuario.novo(
                                        "Matheus",
                                        "matheus@dominio",
                                        "senha123",
                                        Papel.PARTICIPANTE));
        assertEquals("E-mail inválido.", erro.getMessage());
    }

    @Test
    void senhaEObrigatoria() {
        assertThrows(
                UsuarioInvalidoException.class,
                () -> Usuario.novo("Matheus", "matheus@exemplo.com", "", Papel.PARTICIPANTE));
    }

    @Test
    void senhaPrecisaTerOitoCaracteres() {
        var erro =
                assertThrows(
                        UsuarioInvalidoException.class,
                        () ->
                                Usuario.novo(
                                        "Matheus",
                                        "matheus@exemplo.com",
                                        "Abc123!",
                                        Papel.PARTICIPANTE));
        assertEquals("A senha deve ter pelo menos 8 caracteres.", erro.getMessage());
    }

    @Test
    void senhaPrecisaTerLetraENumero() {
        assertThrows(
                UsuarioInvalidoException.class,
                () ->
                        Usuario.novo(
                                "Matheus",
                                "matheus@exemplo.com",
                                "somenteletras",
                                Papel.PARTICIPANTE));
        assertThrows(
                UsuarioInvalidoException.class,
                () ->
                        Usuario.novo(
                                "Matheus",
                                "matheus@exemplo.com",
                                "12345678",
                                Papel.PARTICIPANTE));
    }

    @Test
    void papelEObrigatorio() {
        assertThrows(
                UsuarioInvalidoException.class,
                () -> Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", null));
    }
}
