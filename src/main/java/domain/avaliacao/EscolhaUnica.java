package domain.avaliacao;

import domain.RegraViolada;

import java.util.List;

public record EscolhaUnica(List<String> opcoes) implements TipoResposta {
    public EscolhaUnica {
        opcoes = List.copyOf(opcoes);
        if (opcoes.size() < 2
                || opcoes.stream().anyMatch(s -> s.isBlank() || s.length() > 500)
                || opcoes.stream().distinct().count() != opcoes.size())
            throw new RegraViolada(
                    "Informe pelo menos duas opções distintas e não vazias (até 500 caracteres).");
    }

    public void validar(String v) {
        if (!opcoes.contains(v)) throw new RegraViolada("Escolha uma das opções do questionário.");
    }

    public String nome() {
        return "ESCOLHA_UNICA";
    }
}
