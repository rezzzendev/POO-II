import org.json.JSONObject;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Ponto de entrada do app desktop. Só conversa com o backend via
 * EventoApiClient (HTTP) — nunca importa domain/application/adapter.out
 * de lá (RNF-02).
 */
public class DesktopApp extends JFrame {

    private static final String[] COLUNAS = {"Título", "Modalidade", "Início", "Fim", "Status"};

    private final EventoApiClient api = new EventoApiClient();
    private final DefaultTableModel modeloTabela = new DefaultTableModel(COLUNAS, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);
    private List<JSONObject> eventos = new ArrayList<>();

    public DesktopApp() {
        super("Gestão de Eventos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 440);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(criarBarraDeAcoes(), BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        carregarEventos();
    }

    private JToolBar criarBarraDeAcoes() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);

        JButton novoEvento = new JButton("Novo evento");
        novoEvento.addActionListener(e -> abrirDialogoNovoEvento());
        barra.add(novoEvento);

        JButton atualizar = new JButton("Atualizar lista");
        atualizar.addActionListener(e -> carregarEventos());
        barra.add(atualizar);

        JButton remover = new JButton("Remover selecionado");
        remover.addActionListener(e -> removerSelecionado());
        barra.add(remover);

        return barra;
    }

    private void carregarEventos() {
        try {
            eventos = api.listar();
            modeloTabela.setRowCount(0);
            for (JSONObject evento : eventos) {
                modeloTabela.addRow(new Object[]{
                        evento.getString("titulo"),
                        evento.getString("modalidade"),
                        evento.getString("inicio"),
                        evento.getString("fim"),
                        evento.getString("status")
                });
            }
        } catch (Exception e) {
            eventos = new ArrayList<>();
            modeloTabela.setRowCount(0);
            modeloTabela.addRow(new Object[]{"Sem conexão com a API: " + e.getMessage(), "", "", "", ""});
        }
    }

    private void abrirDialogoNovoEvento() {
        JTextField titulo = new JTextField();
        JTextField descricao = new JTextField();
        JTextField inicio = new JTextField("2026-10-01T09:00:00");
        JTextField fim = new JTextField("2026-10-01T18:00:00");
        JComboBox<String> modalidade = new JComboBox<>(new String[]{"PRESENCIAL", "ONLINE", "HIBRIDO"});

        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(new JLabel("Título"));
        painel.add(titulo);
        painel.add(new JLabel("Descrição"));
        painel.add(descricao);
        painel.add(new JLabel("Início (aaaa-mm-ddThh:mm:ss)"));
        painel.add(inicio);
        painel.add(new JLabel("Fim (aaaa-mm-ddThh:mm:ss)"));
        painel.add(fim);
        painel.add(new JLabel("Modalidade"));
        painel.add(modalidade);

        int escolha = JOptionPane.showConfirmDialog(this, painel, "Novo evento", JOptionPane.OK_CANCEL_OPTION);
        if (escolha != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            api.criar(titulo.getText(), descricao.getText(), inicio.getText(), fim.getText(),
                    (String) modalidade.getSelectedItem());
            carregarEventos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Não deu pra criar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um evento na tabela primeiro.");
            return;
        }

        long id = eventos.get(linha).getLong("id");
        try {
            api.remover(id);
            carregarEventos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Não deu pra remover: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopApp().setVisible(true));
    }
}
