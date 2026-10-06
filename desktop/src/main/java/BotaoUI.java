import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Desenha botões com cor plana. Evita o degradê azul-claro imposto pelo tema Metal,
 * inclusive quando o botão está desabilitado.
 */
public final class BotaoUI extends BasicButtonUI {

    public static ComponentUI createUI(JComponent componente) {
        return new BotaoUI();
    }

    @Override
    protected void installDefaults(AbstractButton botao) {
        super.installDefaults(botao);
        botao.setOpaque(true);
        botao.setContentAreaFilled(true);
        botao.setBorderPainted(true);
    }
}
