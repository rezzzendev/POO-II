package application.avaliacao;

import application.frequencia.FrequenciaService;

import domain.RegraViolada;
import domain.avaliacao.*;

import java.time.Clock;
import java.util.*;

public class AvaliacaoService {
    private final AvaliacaoRepository repo;
    private final FrequenciaService frequencia;
    private final Clock relogio;

    public AvaliacaoService(AvaliacaoRepository r, FrequenciaService f, Clock c) {
        repo = r;
        frequencia = f;
        relogio = c;
    }

    public Questionario criar(Questionario q) {
        frequencia.atividade(q.atividadeId());
        return repo.salvar(q);
    }

    public Questionario buscar(long id) {
        return repo.buscar(id).orElseThrow(() -> new RegraViolada("Questionário não encontrado."));
    }

    public List<Questionario> listar(long a) {
        frequencia.atividade(a);
        return repo.listar(a);
    }

    public void responder(long id, long usuario, Map<Long, String> valores) {
        Questionario q = buscar(id);
        frequencia.exigirInscrito(q.atividadeId(), usuario);
        if (!frequencia.presente(q.atividadeId(), usuario))
            throw new RegraViolada("Presença ainda não validada.");
        q.validarRespostas(valores);
        repo.responder(id, new Avaliacao(usuario, relogio.instant(), valores));
    }

    public List<Avaliacao> resultados(long id) {
        buscar(id);
        return repo.respostas(id);
    }

    public record Resumo(
            long perguntaId,
            String enunciado,
            Map<String, Integer> distribuicao,
            List<String> comentarios,
            Double media) {}

    public List<Resumo> resumo(long id) {
        Questionario q = buscar(id);
        List<Avaliacao> respostas = resultados(id);
        List<Resumo> resultado = new ArrayList<>();
        for (Pergunta p : q.perguntas()) {
            Map<String, Integer> distribuicao = new LinkedHashMap<>();
            List<String> comentarios = new ArrayList<>();
            double soma = 0;
            for (Avaliacao a : respostas) {
                String v = a.respostas().get(p.id());
                if (p.tipo() instanceof Texto) comentarios.add(v);
                else distribuicao.merge(v, 1, Integer::sum);
                if (p.tipo() instanceof Escala) soma += Integer.parseInt(v);
            }
            Double media =
                    p.tipo() instanceof Escala && !respostas.isEmpty()
                            ? soma / respostas.size()
                            : null;
            resultado.add(
                    new Resumo(
                            p.id(),
                            p.enunciado(),
                            Map.copyOf(distribuicao),
                            List.copyOf(comentarios),
                            media));
        }
        return List.copyOf(resultado);
    }
}
