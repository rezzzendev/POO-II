package adapter.out.persistence.inscricao;

import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.PersistenciaException;

import application.evento.EventoRepository;
import application.inscricao.InscricaoRepository;
import application.usuario.UsuarioRepository;

import domain.evento.Evento;
import domain.inscricao.Inscricao;
import domain.inscricao.InscricaoInvalidaException;
import domain.inscricao.StatusInscricao;
import domain.usuario.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InscricaoRepositoryJdbc implements InscricaoRepository {

    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;

    public InscricaoRepositoryJdbc(
            UsuarioRepository usuarioRepository, EventoRepository eventoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
    }

    @Override
    public Inscricao salvar(Inscricao inscricao) {
        return inscricao.getId() == null ? inserir(inscricao) : atualizar(inscricao);
    }

    private Inscricao inserir(Inscricao inscricao) {
        String sql =
                "INSERT INTO inscricoes (usuario_id, evento_id, status, data_inscricao) VALUES (?,"
                        + " ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            conn.setAutoCommit(false);
            stmt.setLong(1, inscricao.getUsuario().getId());
            stmt.setLong(2, inscricao.getEvento().getId());
            stmt.setString(3, inscricao.getStatus().name());
            stmt.setTimestamp(4, Timestamp.valueOf(inscricao.getDataInscricao()));
            stmt.executeUpdate();

            Long id;
            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                chaves.next();
                id = chaves.getLong(1);
            }
            gravarAtividadesEscolhidas(conn, id, inscricao.getAtividadeIds());
            conn.commit();

            return new Inscricao(
                    id,
                    inscricao.getUsuario(),
                    inscricao.getEvento(),
                    inscricao.getAtividadeIds(),
                    inscricao.getStatus(),
                    inscricao.getDataInscricao());
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao salvar inscrição.", e);
        }
    }

    private Inscricao atualizar(Inscricao inscricao) {
        String sql = "UPDATE inscricoes SET status = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            stmt.setString(1, inscricao.getStatus().name());
            stmt.setLong(2, inscricao.getId());
            stmt.executeUpdate();

            gravarAtividadesEscolhidas(conn, inscricao.getId(), inscricao.getAtividadeIds());
            conn.commit();
            return inscricao;
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao atualizar inscrição " + inscricao.getId() + ".", e);
        }
    }

    private void gravarAtividadesEscolhidas(
            Connection conn, Long inscricaoId, List<Long> atividadeIds) throws SQLException {
        try (PreparedStatement excluir =
                conn.prepareStatement("DELETE FROM inscricao_atividades WHERE inscricao_id = ?")) {
            excluir.setLong(1, inscricaoId);
            excluir.executeUpdate();
        }
        if (atividadeIds.isEmpty()) {
            return;
        }
        try (PreparedStatement inserir =
                conn.prepareStatement(
                        "INSERT INTO inscricao_atividades (inscricao_id, atividade_id) VALUES (?,"
                                + " ?)")) {
            for (Long atividadeId : atividadeIds) {
                inserir.setLong(1, inscricaoId);
                inserir.setLong(2, atividadeId);
                inserir.addBatch();
            }
            inserir.executeBatch();
        }
    }

    @Override
    public Optional<Inscricao> buscarPorId(Long id) {
        String sql = "SELECT * FROM inscricoes WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(conn, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar inscrição " + id + ".", e);
        }
    }

    @Override
    public List<Inscricao> listarPorUsuario(Long usuarioId) {
        return listarPor("usuario_id", usuarioId);
    }

    @Override
    public List<Inscricao> listarPorEvento(Long eventoId) {
        return listarPor("evento_id", eventoId);
    }

    private List<Inscricao> listarPor(String coluna, Long valor) {
        String sql = "SELECT * FROM inscricoes WHERE " + coluna + " = ? ORDER BY data_inscricao";
        List<Inscricao> inscricoes = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, valor);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    inscricoes.add(mapear(conn, rs));
                }
            }
            return inscricoes;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar inscrições.", e);
        }
    }

    @Override
    public int contarConfirmadosPorAtividade(Long atividadeId, Long inscricaoIdExcluida) {
        String sql =
                "SELECT COUNT(*) FROM inscricao_atividades ia "
                        + "JOIN inscricoes i ON i.id = ia.inscricao_id "
                        + "WHERE ia.atividade_id = ? AND i.status = 'CONFIRMADA'"
                        + (inscricaoIdExcluida != null ? " AND i.id <> ?" : "");
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, atividadeId);
            if (inscricaoIdExcluida != null) {
                stmt.setLong(2, inscricaoIdExcluida);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao contar inscritos da atividade " + atividadeId + ".", e);
        }
    }

    @Override
    public boolean existeInscricaoAtiva(Long usuarioId, Long eventoId) {
        String sql =
                "SELECT COUNT(*) FROM inscricoes WHERE usuario_id = ? AND evento_id = ? AND status"
                        + " = 'CONFIRMADA'";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, usuarioId);
            stmt.setLong(2, eventoId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao verificar inscrição existente.", e);
        }
    }

    private Inscricao mapear(Connection conn, ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long usuarioId = rs.getLong("usuario_id");
        long eventoId = rs.getLong("evento_id");

        Usuario usuario =
                usuarioRepository
                        .buscarPorId(usuarioId)
                        .orElseThrow(
                                () ->
                                        new InscricaoInvalidaException(
                                                "Usuário " + usuarioId + " não existe mais."));
        Evento evento =
                eventoRepository
                        .buscarPorId(eventoId)
                        .orElseThrow(
                                () ->
                                        new InscricaoInvalidaException(
                                                "Evento " + eventoId + " não existe mais."));

        List<Long> atividadeIds = new ArrayList<>();
        try (PreparedStatement stmt =
                conn.prepareStatement(
                        "SELECT atividade_id FROM inscricao_atividades WHERE inscricao_id = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rsAtividades = stmt.executeQuery()) {
                while (rsAtividades.next()) {
                    atividadeIds.add(rsAtividades.getLong("atividade_id"));
                }
            }
        }

        return new Inscricao(
                id,
                usuario,
                evento,
                atividadeIds,
                StatusInscricao.valueOf(rs.getString("status")),
                rs.getTimestamp("data_inscricao").toLocalDateTime());
    }
}
