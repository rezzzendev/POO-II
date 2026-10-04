import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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

    private static final String[] COLUNAS = {"Título", "Modalidade", "Local", "Início", "Fim", "Status"};

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
        novoEvento.addActionListener(e -> abrirDialogoEvento(null));
        barra.add(novoEvento);

        JButton editarEvento = new JButton("Editar evento");
        editarEvento.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null) abrirDialogoEvento(evento);
                });
        barra.add(editarEvento);

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
        JButton regras = new JButton("Regras de inscrição");
        regras.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null) abrirRegrasInscricao(evento);
                });
        barra.add(regras);

        JButton relatorio = new JButton("Relatórios");
        relatorio.addActionListener(
                e -> {
                    JSONObject evento = selecionado();
                    if (evento != null) {
                        new RelatoriosDialog(this, api, evento).setVisible(true);
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
                                    e.optString("local", "A definir"),
                                    formatarData(e.getString("inicio")),
                                    formatarData(e.getString("fim")),
                                    e.getString("status")
                                });
                });
    }

    private String formatarData(String valor) {
        try {
            return java.time.LocalDateTime.parse(valor)
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (java.time.format.DateTimeParseException e) {
            return valor;
        }
    }

    private void abrirDialogoEvento(JSONObject evento) {
        if (evento != null && !evento.getString("status").equals("RASCUNHO")) {
            JOptionPane.showMessageDialog(
                    this, "Somente eventos em rascunho podem ser editados.");
            return;
        }
        JTextField titulo = new JTextField();
        JTextField descricao = new JTextField();
        String diaInicial = java.time.LocalDate.now().plusDays(1).toString();
        JTextField inicio = new JTextField(diaInicial + "T09:00:00");
        JTextField fim = new JTextField(diaInicial + "T18:00:00");
        JTextField local = new JTextField("A definir");
        JTextField fuso = new JTextField("America/Sao_Paulo");
        JComboBox<String> modalidade =
                new JComboBox<>(new String[] {"PRESENCIAL", "ONLINE", "HIBRIDO"});

        if (evento != null) {
            titulo.setText(evento.optString("titulo"));
            descricao.setText(evento.optString("descricao"));
            inicio.setText(evento.optString("inicio"));
            fim.setText(evento.optString("fim"));
            local.setText(evento.optString("local", "A definir"));
            fuso.setText(evento.optString("fuso", "America/Sao_Paulo"));
            modalidade.setSelectedItem(evento.optString("modalidade", "PRESENCIAL"));
            fuso.setEditable(false);
        }

        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(new JLabel("Título"));
        painel.add(titulo);
        painel.add(new JLabel("Descrição"));
        painel.add(descricao);
        painel.add(new JLabel("Início (aaaa-mm-ddThh:mm:ss)"));
        painel.add(inicio);
        painel.add(new JLabel("Fim (aaaa-mm-ddThh:mm:ss)"));
        painel.add(fim);
        painel.add(new JLabel("Local"));
        painel.add(local);
        painel.add(new JLabel("Fuso IANA (fixo após criação)"));
        painel.add(fuso);
        painel.add(new JLabel("Modalidade"));
        painel.add(modalidade);

        int escolha =
                JOptionPane.showConfirmDialog(
                        this,
                        painel,
                        evento == null ? "Novo evento" : "Editar evento",
                        JOptionPane.OK_CANCEL_OPTION);
        if (escolha != JOptionPane.OK_OPTION) {
            return;
        }

        JSONObject corpo =
                new JSONObject()
                        .put("titulo", titulo.getText().trim())
                        .put("descricao", descricao.getText().trim())
                        .put("inicio", inicio.getText().trim())
                        .put("fim", fim.getText().trim())
                        .put("local", local.getText().trim())
                        .put("fuso", fuso.getText().trim())
                        .put("modalidade", modalidade.getSelectedItem());
        TarefaTela.executar(
                this,
                () ->
                        evento == null
                                ? api.criarEvento(corpo)
                                : api.editarEvento(evento.getLong("id"), corpo),
                resposta -> carregarEventos());
    }

    private void abrirRegrasInscricao(JSONObject evento) {
        long id = evento.getLong("id");
        TarefaTela.executar(
                this,
                () -> api.regrasInscricao(id),
                regras -> exibirEditorRegras(evento, regras));
    }

    private void exibirEditorRegras(JSONObject evento, JSONObject atuais) {
        JCheckBox escolherAtividades =
                new JCheckBox("Participante escolhe atividades", atuais.getBoolean("escolherAtividades"));
        JCheckBox controlarVagas =
                new JCheckBox("Controlar capacidade/vagas", atuais.getBoolean("controlarVagas"));
        JTextField prazo = new JTextField(atuais.getString("prazoCancelamento"));
        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(escolherAtividades);
        painel.add(controlarVagas);
        painel.add(new JLabel("Prazo (aaaa-mm-ddThh:mm:ss; fuso do evento)"));
        painel.add(prazo);
        painel.add(
                new JLabel(
                        "A API bloqueia alterações depois da primeira inscrição para preservar o histórico."));
        if (JOptionPane.showConfirmDialog(
                        this, painel, "Regras — " + evento.getString("titulo"), JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        JSONObject corpo =
                new JSONObject()
                        .put("escolherAtividades", escolherAtividades.isSelected())
                        .put("controlarVagas", controlarVagas.isSelected())
                        .put("prazoCancelamento", prazo.getText().trim());
        TarefaTela.executar(
                this,
                () -> api.configurarRegrasInscricao(evento.getLong("id"), corpo),
                resposta -> JOptionPane.showMessageDialog(this, "Regras de inscrição salvas."));
    }

    private void removerSelecionado() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um evento na tabela primeiro.");
            return;
        }

        long id = eventos.get(linha).getLong("id");
        int resposta =
                JOptionPane.showConfirmDialog(
                        this,
                        "Remover o rascunho selecionado?",
                        "Confirmar remoção",
                        JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) return;
        TarefaTela.executar(this, () -> { api.remover(id); return true; }, ok -> carregarEventos());
    }
}
