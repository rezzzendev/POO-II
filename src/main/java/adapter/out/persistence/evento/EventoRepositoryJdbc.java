package adapter.out.persistence.evento;

import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.PersistenciaException;
import application.evento.EventoRepository;
import domain.evento.Evento;
import domain.evento.Modalidade;
import domain.evento.StatusEvento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventoRepositoryJdbc implements EventoRepository {

    @Override
    public Evento salvar(Evento evento) {
        return evento.getId() == null ? inserir(evento) : atualizar(evento);
    }

    private Evento inserir(Evento evento) {
        String sql = "INSERT INTO eventos (titulo, descricao, inicio, fim, modalidade, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preencher(stmt, evento);
            stmt.executeUpdate();

            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                chaves.next();
                Long id = chaves.getLong(1);
                return new Evento(id, evento.getTitulo(), evento.getDescricao(), evento.getInicio(),
                        evento.getFim(), evento.getModalidade(), evento.getStatus());
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao salvar evento.", e);
        }
    }

    private Evento atualizar(Evento evento) {
        String sql = "UPDATE eventos SET titulo = ?, descricao = ?, inicio = ?, fim = ?, modalidade = ?, status = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            preencher(stmt, evento);
            stmt.setLong(7, evento.getId());
            stmt.executeUpdate();
            return evento;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar evento " + evento.getId() + ".", e);
        }
    }

    private void preencher(PreparedStatement stmt, Evento evento) throws SQLException {
        stmt.setString(1, evento.getTitulo());
        stmt.setString(2, evento.getDescricao());
        stmt.setTimestamp(3, Timestamp.valueOf(evento.getInicio()));
        stmt.setTimestamp(4, Timestamp.valueOf(evento.getFim()));
        stmt.setString(5, evento.getModalidade().name());
        stmt.setString(6, evento.getStatus().name());
    }

    @Override
    public Optional<Evento> buscarPorId(Long id) {
        String sql = "SELECT * FROM eventos WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar evento " + id + ".", e);
        }
    }

    @Override
    public List<Evento> listarTodos() {
        String sql = "SELECT * FROM eventos ORDER BY inicio";
        List<Evento> eventos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                eventos.add(mapear(rs));
            }
            return eventos;
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao listar eventos.", e);
        }
    }

    @Override
    public void remover(Long id) {
        String sql = "DELETE FROM eventos WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao remover evento " + id + ".", e);
        }
    }

    private Evento mapear(ResultSet rs) throws SQLException {
        return new Evento(
                rs.getLong("id"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getTimestamp("inicio").toLocalDateTime(),
                rs.getTimestamp("fim").toLocalDateTime(),
                Modalidade.valueOf(rs.getString("modalidade")),
                StatusEvento.valueOf(rs.getString("status"))
        );
    }
}
