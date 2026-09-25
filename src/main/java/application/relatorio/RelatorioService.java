package application.relatorio;

import application.atividade.AtividadeRepository;
import application.evento.EventoRepository;
import application.frequencia.FrequenciaService;
import application.inscricao.*;

import domain.RegraViolada;

import java.util.*;

/** DTOs de saída imutáveis. CSV fica no adaptador HTTP. */
public class RelatorioService {
    public record Linha(
            long usuarioId,
            String nome,
            String email,
            String inscricao,
            Long atividadeId,
            String atividade,
            boolean presente,
            int marcacoes) {}

    private final InscricaoRepository inscricoes;
    private final AtividadeRepository atividades;
    private final EventoRepository eventos;
    private final RegrasInscricaoRepository regras;
    private final FrequenciaService frequencia;

    public RelatorioService(
            InscricaoRepository i,
            AtividadeRepository a,
            EventoRepository e,
            RegrasInscricaoRepository r,
            FrequenciaService f) {
        inscricoes = i;
        atividades = a;
        eventos = e;
        regras = r;
        frequencia = f;
    }

    public List<Linha> inscritos(long eventoId, Long atividadeId) {
        validar(eventoId, atividadeId);
        List<Linha> linhas = new ArrayList<>();
        boolean escolha =
                regras.buscar(eventos.buscarPorId(eventoId).orElseThrow()).escolherAtividades();
        for (var i : inscricoes.listarPorEvento(eventoId)) {
            if (atividadeId != null && escolha && !i.getAtividadeIds().contains(atividadeId))
                continue;
            var u = i.getUsuario();
            linhas.add(
                    new Linha(
                            u.getId(),
                            u.getNome(),
                            u.getEmail(),
                            i.getStatus().name(),
                            atividadeId,
                            "",
                            false,
                            0));
        }
        return linhas;
    }

    public List<Linha> frequencia(long eventoId, Long atividadeId) {
        validar(eventoId, atividadeId);
        List<Linha> linhas = new ArrayList<>();
        var listaInscricoes = inscricoes.listarPorEvento(eventoId);
        boolean escolha =
                regras.buscar(eventos.buscarPorId(eventoId).orElseThrow()).escolherAtividades();
        for (var a : atividades.listarPorEvento(eventoId)) {
            if (atividadeId != null && !a.getId().equals(atividadeId)) continue;
            for (var i : listaInscricoes) {
                if (escolha && !i.getAtividadeIds().contains(a.getId())) continue;
                var u = i.getUsuario();
                linhas.add(
                        new Linha(
                                u.getId(),
                                u.getNome(),
                                u.getEmail(),
                                i.getStatus().name(),
                                a.getId(),
                                a.getTitulo(),
                                frequencia.presente(a.getId(), u.getId()),
                                frequencia.registros(a.getId(), u.getId()).size()));
            }
        }
        return linhas;
    }

    private void validar(long e, Long a) {
        if (eventos.buscarPorId(e).isEmpty()) throw new RegraViolada("Evento não encontrado.");
        if (a != null
                && atividades.buscarPorId(a).filter(x -> x.getEvento().getId() == e).isEmpty())
            throw new RegraViolada("Atividade não pertence ao evento.");
    }
}
