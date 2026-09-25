import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

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

/**
 * Tela de funcionalidades — só abre depois de autenticar na LoginScreen. Só conversa com o backend
 * via EventoApiClient (HTTP) — nunca importa domain/application/adapter.out de lá (RNF-02).
 */
public class DesktopApp extends JFrame {

    private static final String[] COLUNAS = {"Título", "Modalidade", "Início", "Fim", "Status"};

    private final EventoApiClient api;
    private final DefaultTableModel modeloTabela =
            new DefaultTableModel(COLUNAS, 0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };
    private final JTable tabela = new JTable(modeloTabela);
    private List<JSONObject> eventos = new ArrayList<>();

    public DesktopApp(EventoApiClient api) {
        super("Gestão de Eventos");
        this.api = api;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 550);
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

        JButton atividades = new JButton("Atividades");
        atividades.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null) new AtividadesDialog(this, api, evento).setVisible(true);
                });
        barra.add(atividades);
        JButton publicar = new JButton("Publicar");
        publicar.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null)
                        TarefaTela.executar(
                                this,
                                () -> {
                                    api.publicar(evento.getLong("id"));
                                    return true;
                                },
                                ok -> carregarEventos());
                });
        barra.add(publicar);
        JButton encerrar = new JButton("Encerrar");
        encerrar.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null)
                        TarefaTela.executar(
                                this,
                                () -> {
                                    api.encerrar(evento.getLong("id"));
                                    return true;
                                },
                                ok -> carregarEventos());
                });
        barra.add(encerrar);
        JButton relatorio = new JButton("Exportar frequência");
        relatorio.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null) {
                        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                        chooser.setSelectedFile(new java.io.File("frequencia.csv"));
                        if (chooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION)
                            TarefaTela.executar(
                                    this,
                                    () -> {
                                        java.nio.file.Files.writeString(
                                                chooser.getSelectedFile().toPath(),
                                                api.relatorio(
                                                        evento.getLong("id"), "frequencia", true));
                                        return true;
                                    },
                                    ok -> JOptionPane.showMessageDialog(this, "Relatório salvo."));
                    }
                });
        barra.add(relatorio);
        barra.addSeparator();
        barra.add(new JLabel("Logado como: " + api.usuarioLogado()));

        JButton sair = new JButton("Sair");
        sair.addActionListener(e -> sair());
        barra.add(sair);

        return barra;
    }

    private void sair() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    private JSONObject selecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= eventos.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um evento.");
            return null;
        }
        return eventos.get(linha);
    }

    private void carregarEventos() {
        TarefaTela.executar(
                this,
                api::listar,
                lista -> {
                    eventos = lista;
                    modeloTabela.setRowCount(0);
                    for (JSONObject e : lista)
                        modeloTabela.addRow(
                                new Object[] {
                                    e.getString("titulo"),
                                    e.getString("modalidade"),
                                    e.getString("inicio"),
                                    e.getString("fim"),
                                    e.getString("status")
                                });
                });
    }

    private void abrirDialogoNovoEvento() {
        JTextField titulo = new JTextField();
        JTextField descricao = new JTextField();
        JTextField inicio = new JTextField("2026-10-01T09:00:00");
        JTextField fim = new JTextField("2026-10-01T18:00:00");
        JComboBox<String> modalidade =
                new JComboBox<>(new String[] {"PRESENCIAL", "ONLINE", "HIBRIDO"});

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

        int escolha =
                JOptionPane.showConfirmDialog(
                        this, painel, "Novo evento", JOptionPane.OK_CANCEL_OPTION);
        if (escolha != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            api.criar(
                    titulo.getText(),
                    descricao.getText(),
                    inicio.getText(),
                    fim.getText(),
                    (String) modalidade.getSelectedItem());
            carregarEventos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Não deu pra criar: " + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(
                    this,
                    "Não deu pra remover: " + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
