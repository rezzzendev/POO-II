package adapter.out.persistence.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import adapter.out.persistence.ConnectionFactory;

import domain.usuario.Papel;
import domain.usuario.Usuario;
import domain.usuario.UsuarioInvalidoException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

class UsuarioRepositoryJdbcTest {

    private final UsuarioRepositoryJdbc repository = new UsuarioRepositoryJdbc();

    @BeforeEach
    void limparTabela() throws SQLException {
        try (Connection conn = ConnectionFactory.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM usuarios");
        }
    }

    @Test
    void salvaEBuscaPorEmail() {
        Usuario usuario =
                Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE);

        Usuario salvo = repository.salvar(usuario);

        assertEquals(Long.class, salvo.getId().getClass());
        Optional<Usuario> encontrado = repository.buscarPorEmail("matheus@exemplo.com");
        assertTrue(encontrado.isPresent());
        assertEquals("Matheus", encontrado.get().getNome());
    }

    @Test
    void emailDuplicadoEhRejeitado() {
        repository.salvar(
                Usuario.novo("Matheus", "matheus@exemplo.com", "senha123", Papel.PARTICIPANTE));

        assertThrows(
                UsuarioInvalidoException.class,
                () ->
                        repository.salvar(
                                Usuario.novo(
                                        "Outro Nome",
                                        "matheus@exemplo.com",
                                        "outraSenha",
                                        Papel.PARTICIPANTE)));
    }

    @Test
    void buscaPorEmailInexistenteRetornaVazio() {
        assertTrue(repository.buscarPorEmail("ninguem@exemplo.com").isEmpty());
    }
}
