import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/** Paleta visual compartilhada conceitualmente com o site, sem biblioteca externa. */
public final class Tema {

    public static final Color AZUL = new Color(0x17, 0x3B, 0x57);
    public static final Color AZUL_HOVER = new Color(0x24, 0x58, 0x78);
    public static final Color AZUL_CLARO = new Color(0xE8, 0xF1, 0xF6);
    public static final Color FUNDO = new Color(0xF2, 0xF5, 0xF7);
    public static final Color BORDA = new Color(0xD7, 0xE0, 0xE6);
    public static final Color TEXTO = new Color(0x1B, 0x2D, 0x3A);
    public static final Color BRANCO = Color.WHITE;
    public static final Color PERIGO = new Color(0x8A, 0x30, 0x2B);

    private Tema() {}

    public static void aplicar() {
        try {
            // O tema nativo do Windows ignora a cor de fundo de alguns botões,
            // gerando texto branco sobre fundo branco. O tema Java respeita a paleta.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorado) {
            // O Swing continua funcional com o tema padrão.
        }

        Font fonte = new Font("Segoe UI", Font.PLAIN, 14);
        FontUIResource fonteUi = new FontUIResource(fonte);
        FontUIResource fonteBotao = new FontUIResource(fonte.deriveFont(Font.BOLD));
        UIManager.put("defaultFont", fonteUi);
        UIManager.put("Label.font", fonteUi);
        UIManager.put("Button.font", fonteBotao);
        UIManager.put("ButtonUI", BotaoUI.class.getName());
        UIManager.put("TextField.font", fonteUi);
        UIManager.put("PasswordField.font", fonteUi);
        UIManager.put("ComboBox.font", fonteUi);
        UIManager.put("Table.font", fonteUi);
        UIManager.put("TableHeader.font", fonteBotao);
        UIManager.put("TabbedPane.font", fonteBotao);
        UIManager.put("OptionPane.messageFont", fonteUi);
        UIManager.put("OptionPane.buttonFont", fonteBotao);

        UIManager.put("Panel.background", cor(FUNDO));
        UIManager.put("Label.foreground", cor(TEXTO));
        UIManager.put("Button.background", cor(AZUL));
        UIManager.put("Button.foreground", cor(BRANCO));
        UIManager.put("Button.select", cor(AZUL_HOVER));
        UIManager.put("Button.disabledText", cor(new Color(0xB9, 0xD6, 0xE5)));
        UIManager.put("Button.focus", cor(new Color(0xE6, 0xA5, 0x31)));
        UIManager.put("Button.margin", new Insets(8, 14, 8, 14));
        UIManager.put("TextField.background", cor(BRANCO));
        UIManager.put("TextField.foreground", cor(TEXTO));
        UIManager.put("PasswordField.background", cor(BRANCO));
        UIManager.put("PasswordField.foreground", cor(TEXTO));
        UIManager.put("ComboBox.background", cor(BRANCO));
        UIManager.put("ComboBox.foreground", cor(TEXTO));
        UIManager.put("Table.background", cor(BRANCO));
        UIManager.put("Table.foreground", cor(TEXTO));
        UIManager.put("Table.selectionBackground", cor(AZUL_CLARO));
        UIManager.put("Table.selectionForeground", cor(TEXTO));
        UIManager.put("TableHeader.background", cor(AZUL_CLARO));
        UIManager.put("TableHeader.foreground", cor(AZUL));
        UIManager.put("TabbedPane.selected", cor(BRANCO));
        UIManager.put("TabbedPane.background", cor(FUNDO));
        UIManager.put("TabbedPane.foreground", cor(TEXTO));
        UIManager.put("ScrollPane.background", cor(BRANCO));
        UIManager.put("OptionPane.background", cor(BRANCO));
        UIManager.put("OptionPane.messageForeground", cor(TEXTO));
        UIManager.put("OptionPane.errorDialog.titlePane.background", cor(PERIGO));
    }

    private static ColorUIResource cor(Color cor) {
        return new ColorUIResource(cor);
    }

    public static JPanel cabecalho(String titulo, String subtitulo) {
        JPanel painel = new JPanel();
        painel.setLayout(new javax.swing.BoxLayout(painel, javax.swing.BoxLayout.Y_AXIS));
        painel.setBackground(AZUL);
        painel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setForeground(BRANCO);
        tituloLabel.setFont(tituloLabel.getFont().deriveFont(Font.BOLD, 24f));
        tituloLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(tituloLabel);

        JLabel subtituloLabel = new JLabel(subtitulo);
        subtituloLabel.setForeground(new Color(0xD4, 0xE4, 0xEC));
        subtituloLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        subtituloLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(subtituloLabel);
        return painel;
    }

    public static void aplicarCartao(JComponent componente) {
        componente.setOpaque(true);
        componente.setBackground(BRANCO);
        componente.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDA),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
    }

    public static void botaoPrimario(JButton botao) {
        botao.setUI(new BotaoUI());
        botao.setOpaque(true);
        botao.setContentAreaFilled(true);
        botao.setBackground(AZUL);
        botao.setForeground(BRANCO);
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AZUL),
                BorderFactory.createEmptyBorder(7, 13, 7, 13)));
    }

    public static void botaoSecundario(JButton botao) {
        botao.setUI(new BotaoUI());
        botao.setBackground(BRANCO);
        botao.setForeground(AZUL);
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AZUL),
                BorderFactory.createEmptyBorder(7, 13, 7, 13)));
    }

    public static void botaoPerigo(JButton botao) {
        botao.setUI(new BotaoUI());
        botao.setBackground(BRANCO);
        botao.setForeground(PERIGO);
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PERIGO),
                BorderFactory.createEmptyBorder(7, 13, 7, 13)));
    }

    public static void fundoBranco(Container raiz) {
        for (Component componente : raiz.getComponents()) {
            if (componente instanceof JPanel) componente.setBackground(BRANCO);
            if (componente instanceof Container) fundoBranco((Container) componente);
        }
    }
}
