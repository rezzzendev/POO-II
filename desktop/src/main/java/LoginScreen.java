import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
        setSize(360, 240);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(criarFormulario(), BorderLayout.CENTER);
    }

    private JPanel criarFormulario() {
        JTextField email = new JTextField();
        JPasswordField senha = new JPasswordField();

        JPanel campos = new JPanel(new GridLayout(0, 1, 6, 6));
        campos.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        campos.add(new JLabel("E-mail"));
        campos.add(email);
        campos.add(new JLabel("Senha"));
        campos.add(senha);

        JLabel mensagem = new JLabel(" ");
        mensagem.setForeground(new Color(176, 0, 32));
        mensagem.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JButton entrar = new JButton("Entrar");
        JButton cadastrar = new JButton("Criar conta");

        JPanel botoes = new JPanel();
        botoes.add(entrar);
        botoes.add(cadastrar);

        entrar.addActionListener(
                e -> {
                    try {
                        api.login(email.getText(), new String(senha.getPassword()));
                        dispose();
                        SwingUtilities.invokeLater(() -> new DesktopApp(api).setVisible(true));
                    } catch (Exception ex) {
                        mensagem.setText(ex.getMessage());
                    }
                });

        cadastrar.addActionListener(e -> abrirDialogoCadastro(this));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(mensagem, BorderLayout.NORTH);
        raiz.add(campos, BorderLayout.CENTER);
        raiz.add(botoes, BorderLayout.SOUTH);
        return raiz;
    }

    private void abrirDialogoCadastro(Component pai) {
        JTextField nome = new JTextField();
        JTextField email = new JTextField();
        JPasswordField senha = new JPasswordField();

        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(new JLabel("Nome"));
        painel.add(nome);
        painel.add(new JLabel("E-mail"));
        painel.add(email);
        painel.add(new JLabel("Senha"));
        painel.add(senha);

        int escolha =
                JOptionPane.showConfirmDialog(
                        pai, painel, "Criar conta", JOptionPane.OK_CANCEL_OPTION);
        if (escolha != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            api.cadastrar(nome.getText(), email.getText(), new String(senha.getPassword()));
            JOptionPane.showMessageDialog(pai, "Conta criada. Agora entra com e-mail e senha.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    pai,
                    "Não deu pra cadastrar: " + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }
}
