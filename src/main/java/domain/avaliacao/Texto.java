package domain.avaliacao;

import domain.RegraViolada;

public class Texto implements TipoResposta {
    public void validar(String v) {
        if (v == null || v.isBlank() || v.length() > 4000)
            throw new RegraViolada("Resposta textual obrigatória, com até 4000 caracteres.");
    }

    public String nome() {
        return "TEXTO";
    }
}
