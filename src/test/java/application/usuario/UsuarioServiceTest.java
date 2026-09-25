package application.usuario;

import static org.junit.jupiter.api.Assertions.*;

import domain.usuario.*;

import org.junit.jupiter.api.Test;

import java.util.*;

/** Porta substituída por memória: caso de uso testado sem HTTP, Swing ou banco. */
class UsuarioServiceTest {
    private final UsuarioRepository memoria =
            new UsuarioRepository() {
                private final Map<Long, Usuario> dados = new HashMap<>();

                public Usuario salvar(Usuario u) {
                    long id = u.getId() == null ? dados.size() + 1 : u.getId();
                    Usuario salvo =
                            new Usuario(
                                    id, u.getNome(), u.getEmail(), u.getSenhaHash(), u.getPapel());
                    dados.put(id, salvo);
                    return salvo;
                }

                public Optional<Usuario> buscarPorId(Long id) {
                    return Optional.ofNullable(dados.get(id));
                }

                public Optional<Usuario> buscarPorEmail(String e) {
                    return dados.values().stream()
                            .filter(u -> u.getEmail().equalsIgnoreCase(e.strip()))
                            .findFirst();
                }
            };

    @Test
    void cadastroLoginEPerfilSemInterface() {
        var service = new UsuarioService(memoria);
        var u = service.cadastrar("Ana", " ANA@TEST.LOCAL ", "senha");
        assertEquals(Papel.PARTICIPANTE, u.getPapel());
        assertEquals("ana@test.local", u.getEmail());
        assertNotEquals("senha", u.getSenhaHash());
        assertNotNull(service.autenticar("ana@test.local", "senha"));
        assertNull(service.autenticar("ana@test.local", "errada"));
        assertThrows(
                UsuarioInvalidoException.class,
                () -> service.cadastrar("Outra", "ana@test.local", "senha"));
        service.editar(u, "Ana Silva", "ana@test.local");
        assertEquals("Ana Silva", memoria.buscarPorId(u.getId()).orElseThrow().getNome());
    }
}
