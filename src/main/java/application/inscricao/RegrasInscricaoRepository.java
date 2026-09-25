package application.inscricao;

import domain.evento.Evento;
import domain.inscricao.RegrasInscricao;

public interface RegrasInscricaoRepository {
    RegrasInscricao buscar(Evento evento);

    void salvar(long eventoId, RegrasInscricao regras);
}
