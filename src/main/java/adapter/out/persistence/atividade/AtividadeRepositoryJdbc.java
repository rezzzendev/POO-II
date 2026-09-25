package adapter.out.persistence.atividade;

import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.PersistenciaException;

import application.atividade.AtividadeRepository;
import application.evento.EventoRepository;
import application.usuario.UsuarioRepository;

import domain.atividade.Atividade;
import domain.atividade.AtividadeInvalidaException;
import domain.atividade.VinculoPessoa;
import domain.evento.Evento;
import domain.usuario.Usuario;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Depende de EventoRepository e UsuarioRepository (as portas, não o JDBC direto) pra hidratar
 * Evento e nome de pessoa vinculada.
 */
public class AtividadeRepositoryJdbc implements AtividadeRepository {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public AtividadeRepositoryJdbc(
            EventoRepository eventoRepository, UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Atividade salvar(Atividade atividade) {
        return atividade.getId() == null ? inserir(atividade) : atualizar(atividade);
    }

    private Atividade inserir(Atividade atividade) {
        String sql =
                "INSERT INTO atividades (titulo, descricao, tipo, trilha, local, inicio, fim,"
                        + " capacidade, evento_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt =
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preencher(stmt, atividade);
            stmt.executeUpdate();

            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                chaves.next();
                Long id = chaves.getLong(1);
                return new Atividade(
                        id,
                        atividade.getTitulo(),
                        atividade.getDescricao(),
                        atividade.getTipo(),
                        atividade.getTrilha(),
                        atividade.getLocal(),
                        atividade.getInicio(),
                        atividade.getFim(),
                        atividade.getCapacidade(),
                        atividade.getEvento());
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao salvar atividade.", e);
        }
    }

    private Atividade atualizar(Atividade atividade) {
        String sql =
                "UPDATE atividades SET titulo=?, descricao=?, tipo=?, trilha=?, local=?, inicio=?,"
                        + " fim=?, capacidade=?, evento_id=? WHERE id=?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            preencher(stmt, atividade);
            stmt.setLong(10, atividade.getId());
            stmt.executeUpdate();
            return atividade;
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao atualizar atividade " + atividade.getId() + ".", e);
        }
    }

    private void preencher(PreparedStatement stmt, Atividade atividade) throws SQLException {
        stmt.setString(1, atividade.getTitulo());
        stmt.setString(2, atividade.getDescricao());
        stmt.setString(3, atividade.getTipo());
        stmt.setString(4, atividade.getTrilha());
        stmt.setString(5, atividade.getLocal());
        stmt.setTimestamp(6, Timestamp.valueOf(atividade.getInicio()));
        stmt.setTimestamp(7, Timestamp.valueOf(atividade.getFim()));
        if (atividade.getCapacidade() == null) {
            stmt.setNull(8, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(8, atividade.getCapacidade());
        }
        stmt.setLong(9, atividade.getEvento().getId());
    }

    @Override
    public Optional<Atividade> buscarPorId(Long id) {
        String sql = "SELECT * FROM atividades WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar atividade " + id + ".", e);
        }
    }

    @Override
    public List<Atividade> buscar(
            Long eventoId, LocalDate data, String trilha, String tipo, String local) {
        StringBuilder sql = new StringBuilder("SELECT * FROM atividades WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (eventoId != null) {
            sql.append(" AND evento_id = ?");
            parametros.add(eventoId);
        }
        if (data != null) {
            sql.append(" AND CAST(inicio AS DATE) = ?");
            parametros.add(Date.valueOf(data));
        }
        if (trilha != null) {
            sql.append(" AND trilha = ?");
            parametros.add(trilha);
        }
        if (tipo != null) {
            sql.append(" AND tipo = ?");
            parametros.add(tipo);
        }
        if (local != null) {
            sql.append(" AND local = ?");
            parametros.add(local);
        }
        sql.append(" ORDER BY inicio");

        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<Atividade> atividades = new ArrayList<>();
                while (rs.next()) {
                    atividades.add(mapear(rs));
                }
                return atividades;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao buscar atividades.", e);
        }
    }

    @Override
    public void remover(Long id) {
        String sql = "DELETE FROM atividades WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao remover atividade " + id + ".", e);
        }
    }

    @Override
    public void vincularPessoa(Long atividadeId, VinculoPessoa vinculo) {
        String sql =
                "MERGE INTO atividade_pessoas (atividade_id, usuario_id, papel) KEY (atividade_id,"
                        + " usuario_id, papel) VALUES (?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, atividadeId);
            stmt.setLong(2, vinculo.getUsuarioId());
            stmt.setString(3, vinculo.getPapel());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao vincular pessoa à atividade " + atividadeId + ".", e);
        }
    }

    @Override
    public List<VinculoPessoa> listarPessoas(Long atividadeId) {
        String sql = "SELECT usuario_id, papel FROM atividade_pessoas WHERE atividade_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, atividadeId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<VinculoPessoa> vinculos = new ArrayList<>();
                while (rs.next()) {
                    long usuarioId = rs.getLong("usuario_id");
                    Usuario usuario =
                            usuarioRepository
                                    .buscarPorId(usuarioId)
                                    .orElseThrow(
                                            () ->
                                                    new AtividadeInvalidaException(
                                                            "Usuário "
                                                                    + usuarioId
                                                                    + " não existe mais."));
                    vinculos.add(
                            new VinculoPessoa(usuarioId, usuario.getNome(), rs.getString("papel")));
                }
                return vinculos;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao listar pessoas da atividade " + atividadeId + ".", e);
        }
    }

    private Atividade mapear(ResultSet rs) throws SQLException {
        long eventoId = rs.getLong("evento_id");
        Evento evento =
                eventoRepository
                        .buscarPorId(eventoId)
                        .orElseThrow(
                                () ->
                                        new AtividadeInvalidaException(
                                                "Evento "
                                                        + eventoId
                                                        + " da atividade não existe mais."));

        int capacidadeLida = rs.getInt("capacidade");
        Integer capacidade = rs.wasNull() ? null : capacidadeLida;

        return new Atividade(
                rs.getLong("id"),
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getString("tipo"),
                rs.getString("trilha"),
                rs.getString("local"),
                rs.getTimestamp("inicio").toLocalDateTime(),
                rs.getTimestamp("fim").toLocalDateTime(),
                capacidade,
                evento);
    }
}
