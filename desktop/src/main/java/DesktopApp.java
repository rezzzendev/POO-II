import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/**
 * Ponto de entrada do app desktop. As linhas da tabela ainda são de exemplo —
 * passam a vir da API de verdade (via HttpClient) quando o adapter.in.api
 * existir. Não importa nada de domain/application/adapter.out (RNF-02).
 */
public class DesktopApp extends JFrame {

    private static final String[] COLUNAS = {"Título", "Modalidade", "Início", "Fim", "Status"};

    private static final Object[][] EVENTOS_EXEMPLO = {
            {"Semana de Tecnologia", "PRESENCIAL", "01/10/2026 09:00", "01/10/2026 18:00", "RASCUNHO"},
            {"Workshop de Java", "ONLINE", "15/10/2026 19:00", "15/10/2026 21:00", "PUBLICADO"},
    };

    public DesktopApp() {
        super("Gestão de Eventos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(criarBarraDeAcoes(), BorderLayout.NORTH);
        add(new JScrollPane(criarTabelaDeEventos()), BorderLayout.CENTER);
    }

    private JToolBar criarBarraDeAcoes() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);

        JButton novoEvento = new JButton("Novo evento");
        novoEvento.setEnabled(false);
        novoEvento.setToolTipText("Disponível quando a API existir");
        barra.add(novoEvento);

        return barra;
    }

    private JTable criarTabelaDeEventos() {
        DefaultTableModel modelo = new DefaultTableModel(EVENTOS_EXEMPLO, COLUNAS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        return new JTable(modelo);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopApp().setVisible(true));
    }
}
