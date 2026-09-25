package domain.avaliacao;

import domain.RegraViolada;

public record Pergunta(long id, String enunciado, TipoResposta tipo) {
    public Pergunta {
        if (enunciado == null || enunciado.isBlank() || enunciado.length() > 1000 || tipo == null)
            throw new RegraViolada("Pergunta exige enunciado (até 1000 caracteres) e tipo.");
    }

    public void validar(String valor) {
        tipo.validar(valor);
    }
}
