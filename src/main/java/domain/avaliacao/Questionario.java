package domain.avaliacao;

import domain.RegraViolada;

import java.util.*;

/** Agregado imutável: perguntas não mudam depois de publicado, preservando respostas anteriores. */
public record Questionario(long id, long atividadeId, String titulo, List<Pergunta> perguntas) {
    public static final String POLITICA =
            "Respostas identificadas, acessíveis somente à organização para avaliar e melhorar as"
                    + " atividades. Não são publicadas no site.";

    public Questionario {
        perguntas = List.copyOf(perguntas);
        if (atividadeId <= 0
                || titulo == null
                || titulo.isBlank()
                || titulo.length() > 255
                || perguntas.isEmpty()
                || perguntas.size() > 50)
            throw new RegraViolada(
                    "Questionário exige título (até 255 caracteres), atividade e de 1 a 50"
                            + " perguntas.");
    }

    public void validarRespostas(Map<Long, String> valores) {
        if (valores.size() != perguntas.size())
            throw new RegraViolada("Responda todas as perguntas, sem campos extras.");
        for (Pergunta p : perguntas) p.validar(valores.get(p.id()));
    }
}
