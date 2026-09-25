package adapter.out.persistence.usuario;

import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.PersistenciaException;

import application.usuario.UsuarioRepository;

import domain.usuario.Papel;
import domain.usuario.Usuario;
import domain.usuario.UsuarioInvalidoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UsuarioRepositoryJdbc implements UsuarioRepository {

    private static final String ESTADO_VIOLACAO_UNICIDADE = "23505";

    @Override
    public Usuario salvar(Usuario usuario) {
        return usuario.getId() == null ? inserir(usuario) : atualizar(usuario);
    }

    private Usuario inserir(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nome, email, senha_hash, papel) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preencher(stmt, usuario);
            stmt.executeUpdate();

            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                chaves.next();
                Long id = chaves.getLong(1);
                return new Usuario(
                        id,
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getSenhaHash(),
                        usuario.getPapel());
            }
        } catch (SQLException e) {
            if (ESTADO_VIOLACAO_UNICIDADE.equals(e.getSQLState())) {
                throw new UsuarioInvalidoException("Já existe uma conta com esse e-mail.");
            }
            throw new PersistenciaException("Erro ao salvar usuário.", e);
        }
    }

    private Usuario atualizar(Usuario usuario) {
        String sql =
                "UPDATE usuarios SET nome = ?, email = ?, senha_hash = ?, papel = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            preencher(stmt, usuario);
            stmt.setLong(5, usuario.getId());
            stmt.executeUpdate();
            return usuario;
        } catch (SQLException e) {
            if (ESTADO_VIOLACAO_UNICIDADE.equals(e.getSQLState())) {
                throw new UsuarioInvalidoException("Já existe uma conta com esse e-mail.");
            }
            throw new PersistenciaException(
                    "Erro ao atualizar usuário " + usuario.getId() + ".", e);
        }
    }

    private void preencher(PreparedStatement stmt, Usuario usuario) throws SQLException {
        stmt.setString(1, usuario.getNome());
        stmt.setString(2, usuario.getEmail());
        stmt.setString(3, usuario.getSenhaHash());
        stmt.setString(4, usuario.getPapel().name());
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar usuário " + id + ".", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE LOWER(email) = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.strip().toLowerCase(java.util.Locale.ROOT));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar usuário por e-mail.", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha_hash"),
                Papel.valueOf(rs.getString("papel")));
    }
}
