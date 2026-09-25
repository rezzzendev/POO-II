package adapter.out.persistence.inscricao;

import adapter.out.persistence.Sql;

import application.inscricao.RegrasInscricaoRepository;

import domain.evento.Evento;
import domain.inscricao.RegrasInscricao;

public class RegrasInscricaoJdbc implements RegrasInscricaoRepository {
    public RegrasInscricao buscar(Evento e) {
        return Sql.listar(
                        "SELECT * FROM regras_inscricao WHERE evento_id=?",
                        r ->
                                new RegrasInscricao(
                                        r.getBoolean("escolher_atividades"),
                                        r.getBoolean("controlar_vagas"),
                                        r.getTimestamp("prazo_cancelamento").toLocalDateTime()),
                        e.getId())
                .stream()
                .findFirst()
                .orElse(new RegrasInscricao(true, true, e.getInicio()));
    }

    public void salvar(long id, RegrasInscricao r) {
        Sql.executar(
                "MERGE INTO regras_inscricao KEY(evento_id) VALUES (?,?,?,?)",
                id,
                r.escolherAtividades(),
                r.controlarVagas(),
                r.prazoCancelamento());
    }
}
