package domain;

/** Falha esperada de negócio; a API converte em mensagem para o usuário. */
public class RegraViolada extends IllegalArgumentException {
    public RegraViolada(String mensagem) {
        super(mensagem);
    }
}
