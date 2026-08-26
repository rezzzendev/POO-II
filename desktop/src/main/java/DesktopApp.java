import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Ponto de entrada do app desktop. Ainda não conversa com a API — isso entra
 * quando o backend tiver o adaptador adapter.in.api pronto (depende do
 * domínio de Evento estar modelado primeiro).
 */
public class DesktopApp extends JFrame {

    public DesktopApp() {
        super("Gestão de Eventos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 320);
        setLocationRelativeTo(null);
        add(new JLabel("Em construção — próximo passo: modelar Evento.", JLabel.CENTER));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopApp().setVisible(true));
    }
}
