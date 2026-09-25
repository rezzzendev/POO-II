import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.swing.*;

/** Rede fora da thread gráfica, resultado aplicado na thread do Swing. */
public final class TarefaTela {
    private TarefaTela() {}

    public static <T> void executar(Component pai, Callable<T> trabalho, Consumer<T> sucesso) {
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception {
                return trabalho.call();
            }

            protected void done() {
                try {
                    sucesso.accept(get());
                } catch (Exception e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    JOptionPane.showMessageDialog(
                            pai,
                            causa.getMessage(),
                            "Não foi possível concluir",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
