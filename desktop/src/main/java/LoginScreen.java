import java.awt.BorderLayout;
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

        JButton entrar = new JButton("Entrar");
        JButton cadastrar = new JButton("Criar conta");

        JPanel botoes = new JPanel();
        botoes.add(entrar);
        botoes.add(cadastrar);

        entrar.addActionListener(
                e -> {
                    entrar.setEnabled(false);
                    cadastrar.setEnabled(false);
                    String emailDigitado = email.getText().trim();
                    String senhaDigitada = new String(senha.getPassword());
                    TarefaTela.executar(
                            this,
                            () -> {
                                api.login(emailDigitado, senhaDigitada);
                                return true;
                            },
                            ok -> {
                                dispose();
                                SwingUtilities.invokeLater(() -> new DesktopApp(api).setVisible(true));
                            },
                            () -> {
                                if (isDisplayable()) {
                                    entrar.setEnabled(true);
                                    cadastrar.setEnabled(true);
                                }
                            });
                });

        cadastrar.addActionListener(e -> abrirDialogoCadastro(this));

        JPanel raiz = new JPanel(new BorderLayout());
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
        painel.add(new JLabel("E-mail (exemplo: nome@dominio.com)"));
        painel.add(email);
        painel.add(new JLabel("Senha"));
        painel.add(senha);
        painel.add(new JLabel("Use pelo menos 8 caracteres, com letras e números."));

        int escolha =
                JOptionPane.showConfirmDialog(
                        pai, painel, "Criar conta", JOptionPane.OK_CANCEL_OPTION);
        if (escolha != JOptionPane.OK_OPTION) {
            return;
        }

        String nomeDigitado = nome.getText().trim();
        String emailDigitado = email.getText().trim();
        String senhaDigitada = new String(senha.getPassword());
        TarefaTela.executar(
                pai,
                () -> {
                    api.cadastrar(nomeDigitado, emailDigitado, senhaDigitada);
                    return true;
                },
                ok -> JOptionPane.showMessageDialog(pai, "Conta criada. Agora entre com e-mail e senha."));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        javax.swing.UIManager.setLookAndFeel(
                                javax.swing.UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception ignorado) {
                        // Mantém o visual padrão do Swing caso o tema do sistema não esteja disponível.
                    }
                    new LoginScreen().setVisible(true);
                });
    }
}
