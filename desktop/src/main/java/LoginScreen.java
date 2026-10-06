import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Primeira tela do desktop. Só depois de autenticar aqui é que a tela de funcionalidades
 * (DesktopApp) abre.
 */
public class LoginScreen extends JFrame {

    private final EventoApiClient api = new EventoApiClient();

    public LoginScreen() {
        super("Gestão de Eventos — Entrar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440, 390));
        setSize(440, 390);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Tema.cabecalho(
                "Gestão de eventos",
                "Acesso do administrador e do organizador"), BorderLayout.NORTH);
        add(criarFormulario(), BorderLayout.CENTER);
    }

    private JPanel criarFormulario() {
        JTextField email = new JTextField();
        JPasswordField senha = new JPasswordField();

        JPanel campos = new JPanel(new GridLayout(0, 1, 8, 8));
        campos.setBackground(Tema.BRANCO);
        campos.setBorder(BorderFactory.createEmptyBorder(20, 20, 12, 20));
        campos.add(new JLabel("E-mail"));
        campos.add(email);
        campos.add(new JLabel("Senha"));
        campos.add(senha);

        JButton entrar = new JButton("Entrar");
        Tema.botaoPrimario(entrar);

        JPanel botoes = new JPanel();
        botoes.setBackground(Tema.BRANCO);
        botoes.setBorder(BorderFactory.createEmptyBorder(0, 14, 18, 14));
        botoes.add(entrar);

        entrar.addActionListener(
                e -> {
                    entrar.setEnabled(false);
                    String emailDigitado = email.getText().trim();
                    String senhaDigitada = new String(senha.getPassword());
                    TarefaTela.executar(
                            this,
                            () -> {
                                api.login(emailDigitado, senhaDigitada);
                                if ("PARTICIPANTE".equals(api.papelLogado())) {
                                    api.sair();
                                    throw new IllegalArgumentException(
                                            "Participantes usam o site. Entre aqui com uma conta de organizador ou administrador.");
                                }
                                return true;
                            },
                            ok -> {
                                dispose();
                                SwingUtilities.invokeLater(() -> new DesktopApp(api).setVisible(true));
                            },
                            () -> {
                                if (isDisplayable()) {
                                    entrar.setEnabled(true);
                                }
                            });
                });

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.BRANCO);
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 28, 24, 28));
        raiz.add(campos, BorderLayout.CENTER);
        raiz.add(botoes, BorderLayout.SOUTH);
        return raiz;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    Tema.aplicar();
                    new LoginScreen().setVisible(true);
                });
    }
}
