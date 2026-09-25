package domain.avaliacao;

import domain.RegraViolada;

public record Escala(int minimo, int maximo) implements TipoResposta {
    public Escala {
        if (minimo >= maximo) throw new RegraViolada("Escala exige mínimo menor que máximo.");
    }

    public void validar(String v) {
        try {
            int n = Integer.parseInt(v);
            if (n < minimo || n > maximo) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new RegraViolada("Informe um inteiro entre " + minimo + " e " + maximo + ".");
        }
    }

    public String nome() {
        return "ESCALA";
    }
}
