package application.frequencia;

import application.atividade.AtividadeRepository;
import application.inscricao.*;

import domain.*;
import domain.atividade.Atividade;
import domain.frequencia.*;
import domain.inscricao.StatusInscricao;

import java.time.*;
import java.util.*;

public class FrequenciaService {
    private final FrequenciaRepository repo;
    private final AtividadeRepository atividades;
    private final InscricaoRepository inscricoes;
    private final RegrasInscricaoRepository regras;
    private final Clock relogio;

    public FrequenciaService(
            FrequenciaRepository r,
            AtividadeRepository a,
            InscricaoRepository i,
            RegrasInscricaoRepository regras,
            Clock c) {
        repo = r;
        atividades = a;
        inscricoes = i;
        this.regras = regras;
        relogio = c;
    }

    public Atividade atividade(long id) {
        return atividades
                .buscarPorId(id)
                .orElseThrow(() -> new RegraViolada("Atividade não encontrada."));
    }

    public void exigirInscrito(long a, long u) {
        Atividade atividade = atividade(a);
        boolean selecao = regras.buscar(atividade.getEvento()).escolherAtividades();
        if (inscricoes.listarPorUsuario(u).stream()
                .noneMatch(
                        i ->
                                i.getStatus() == StatusInscricao.CONFIRMADA
                                        && i.getEvento().equals(atividade.getEvento())
                                        && (!selecao || i.getAtividadeIds().contains(a))))
            throw new RegraViolada("Participante não está inscrito nesta atividade.");
    }

    public synchronized void configurar(long a, String politica) {
        atividade(a);
        Frequencia.politica(politica);
        if (repo.possuiHistorico(a))
            throw new RegraViolada(
                    "Política bloqueada após a primeira frequência para preservar o histórico.");
        repo.configurar(a, politica);
    }

    public String politica(long a) {
        atividade(a);
        return repo.politica(a);
    }

    public synchronized CodigoFrequencia gerar(long a, TipoMarcacao t) {
        atividade(a);
        if (!Frequencia.politica(repo.politica(a)).aceita(t))
            throw new RegraViolada("Operação incompatível com a política.");
        CodigoFrequencia codigo =
                new CodigoFrequencia(
                        UUID.randomUUID().toString(), a, t, relogio.instant().plusSeconds(300));
        repo.salvarCodigo(codigo);
        return codigo;
    }

    public synchronized void registrarQr(long usuario, String token) {
        CodigoFrequencia c =
                repo.codigo(token).orElseThrow(() -> new RegraViolada("QR Code inválido."));
        c.validar(relogio.instant());
        exigirInscrito(c.atividadeId(), usuario);
        frequencia(c.atividadeId(), usuario).validarQr(c.tipo());
        repo.registrar(
                new RegistroFrequencia(
                        0,
                        c.atividadeId(),
                        usuario,
                        c.tipo(),
                        "QR",
                        usuario,
                        relogio.instant(),
                        ""));
    }

    public synchronized void manual(
            long a, long u, long responsavel, boolean presente, String motivo) {
        exigirInscrito(a, u);
        repo.registrar(
                new RegistroFrequencia(
                        0,
                        a,
                        u,
                        presente ? TipoMarcacao.VALIDACAO_MANUAL : TipoMarcacao.INVALIDACAO_MANUAL,
                        "MANUAL",
                        responsavel,
                        relogio.instant(),
                        motivo));
    }

    public boolean presente(long a, long u) {
        return frequencia(a, u).presente();
    }

    public List<RegistroFrequencia> registros(long a, long u) {
        atividade(a);
        return repo.registros(a, u);
    }

    private Frequencia frequencia(long a, long u) {
        return new Frequencia(Frequencia.politica(repo.politica(a)), repo.registros(a, u));
    }
}
