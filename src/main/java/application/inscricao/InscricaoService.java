package application.inscricao;

import application.atividade.AtividadeRepository;
import application.evento.EventoRepository;

import domain.RegraViolada;
import domain.atividade.Atividade;
import domain.evento.*;
import domain.inscricao.*;
import domain.usuario.*;

import java.time.*;
import java.util.*;

/**
 * Caso de uso sem HTTP/JDBC. Uma instância compartilhada serializa a reserva de vagas na API local.
 */
public class InscricaoService {
    private final InscricaoRepository inscricoes;
    private final EventoRepository eventos;
    private final AtividadeRepository atividades;
    private final RegrasInscricaoRepository regras;
    private final Clock relogio;

    public InscricaoService(
            InscricaoRepository i,
            EventoRepository e,
            AtividadeRepository a,
            RegrasInscricaoRepository r,
            Clock c) {
        inscricoes = i;
        eventos = e;
        atividades = a;
        regras = r;
        relogio = c;
    }

    public synchronized Inscricao inscrever(Usuario u, long eventoId, List<Long> ids) {
        Evento e = evento(eventoId);
        if (inscricoes.existeInscricaoAtiva(u.getId(), eventoId))
            throw new RegraViolada("Você já está inscrito neste evento.");
        validar(u, e, ids, null);
        return inscricoes.salvar(Inscricao.nova(u, e, ids));
    }

    public synchronized Inscricao selecionar(Usuario u, long id, List<Long> ids) {
        Inscricao i = obter(id);
        if (!i.getUsuario().equals(u))
            throw new RegraViolada("Somente o titular pode alterar sua agenda.");
        validar(u, i.getEvento(), ids, id);
        i.definirAtividades(ids);
        return inscricoes.salvar(i);
    }

    public synchronized Inscricao cancelar(Usuario u, long id) {
        Inscricao i = obter(id);
        if (!i.getUsuario().equals(u) && u.getPapel() == Papel.PARTICIPANTE)
            throw new RegraViolada("Inscrição de outro participante.");
        regras.buscar(i.getEvento())
                .validarCancelamento(
                        LocalDateTime.ofInstant(relogio.instant(), i.getEvento().getFuso()));
        i.cancelar();
        return inscricoes.salvar(i);
    }

    public synchronized void configurar(long eventoId, RegrasInscricao r) {
        Evento e = evento(eventoId);
        if (!inscricoes.listarPorEvento(eventoId).isEmpty())
            throw new RegraViolada(
                    "Regras bloqueadas após a primeira inscrição para preservar o histórico.");
        if (r.prazoCancelamento().isAfter(e.getFim()))
            throw new RegraViolada("Prazo deve estar dentro do período do evento.");
        regras.salvar(eventoId, r);
    }

    public RegrasInscricao regras(long id) {
        return regras.buscar(evento(id));
    }

    private Evento evento(long id) {
        return eventos.buscarPorId(id)
                .orElseThrow(() -> new RegraViolada("Evento não encontrado."));
    }

    private Inscricao obter(long id) {
        return inscricoes
                .buscarPorId(id)
                .orElseThrow(() -> new RegraViolada("Inscrição não encontrada."));
    }

    private void validar(Usuario u, Evento e, List<Long> ids, Long ignorar) {
        if (e.getStatus() != StatusEvento.PUBLICADO)
            throw new RegraViolada("Evento não está aberto para inscrições.");
        RegrasInscricao r = regras.buscar(e);
        r.validarEscolha(ids);
        List<Atividade> agenda = new ArrayList<>();
        for (Inscricao anterior : inscricoes.listarPorUsuario(u.getId())) {
            if (anterior.getStatus() == StatusInscricao.CONFIRMADA
                    && !Objects.equals(anterior.getId(), ignorar))
                for (Long id : anterior.getAtividadeIds())
                    atividades.buscarPorId(id).ifPresent(agenda::add);
        }
        for (Long id : ids) {
            Atividade a =
                    atividades
                            .buscarPorId(id)
                            .orElseThrow(() -> new RegraViolada("Atividade não encontrada."));
            if (!a.getEvento().equals(e))
                throw new RegraViolada("A atividade pertence a outro evento.");
            if (r.controlarVagas()
                    && !a.temVagaDisponivel(inscricoes.contarConfirmadosPorAtividade(id, ignorar)))
                throw new RegraViolada("Vagas esgotadas: " + a.getTitulo());
            for (Atividade outra : agenda) {
                Instant inicio = a.getInicio().atZone(e.getFuso()).toInstant(),
                        fim = a.getFim().atZone(e.getFuso()).toInstant();
                if (inicio.isBefore(outra.getFim().atZone(outra.getEvento().getFuso()).toInstant())
                        && outra.getInicio()
                                .atZone(outra.getEvento().getFuso())
                                .toInstant()
                                .isBefore(fim))
                    throw new RegraViolada("Conflito de horário: " + outra.getTitulo());
            }
            agenda.add(a);
        }
    }
}
