import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.*;

/** Rede fora da thread gráfica, resultado aplicado na thread do Swing. */
public final class TarefaTela {
    private TarefaTela() {}

    public static <T> void executar(Component pai, Callable<T> trabalho, Consumer<T> sucesso) {
        executar(pai, trabalho, sucesso, () -> {});
    }

    public static <T> void executar(
            Component pai, Callable<T> trabalho, Consumer<T> sucesso, Runnable conclusao) {
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception {
                return trabalho.call();
            }

            protected void done() {
                try {
                    sucesso.accept(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            pai,
                            mensagem(e),
                            "Não foi possível concluir",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    conclusao.run();
                }
            }
        }.execute();
    }

    private static String mensagem(Throwable erro) {
        Throwable atual = erro;
        String encontrada = null;
        while (atual != null) {
            if (atual instanceof java.net.ConnectException) {
                return "A API não está ligada. Inicie o servidor em http://localhost:8080.";
            }
            if (atual.getMessage() != null && !atual.getMessage().isBlank()) {
                encontrada = atual.getMessage();
            }
            atual = atual.getCause();
        }
        return encontrada == null ? "Ocorreu um erro inesperado." : encontrada;
    }
}
