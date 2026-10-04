import org.json.JSONObject;
import org.json.JSONArray;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/** Painel administrativo em uma única janela; todas as operações passam pelo cliente HTTP. */
public class DesktopApp extends JFrame {

    private static final String[] COLUNAS = {"Título", "Modalidade", "Local", "Início", "Status"};

    private final EventoApiClient api;
    private final DefaultTableModel modeloTabela =
            new DefaultTableModel(COLUNAS, 0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };
    private final JTable tabela = new JTable(modeloTabela);
    private final JLabel eventoSelecionado = new JLabel("Selecione um evento na lista.");
    private final List<JSONObject> eventos = new ArrayList<>();
    private final AtividadesPainel painelAtividades;
    private final QuestionariosPainel painelQuestionarios;
    private final RelatoriosPainel painelRelatorios;
    private final JComboBox<String> atividadeQuestionario = new JComboBox<>();
    private final List<Long> atividadesQuestionarioIds = new ArrayList<>();
    private long sequenciaAtividades;
    private boolean atualizandoComboAtividades;

    public DesktopApp(EventoApiClient api) {
        super("Gestão de Eventos — Administração");
        this.api = api;
        painelAtividades = new AtividadesPainel(api);
        painelQuestionarios = new QuestionariosPainel(api);
        painelRelatorios = new RelatoriosPainel(api);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        JPanel raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        raiz.add(criarCabecalho(), BorderLayout.NORTH);
        raiz.add(criarConteudo(), BorderLayout.CENTER);
        add(raiz);

        tabela.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(true);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) atualizarContexto();
        });
        carregarEventos();
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(12, 4));
        JLabel titulo = new JLabel("Painel de administração");
        titulo.setFont(titulo.getFont().deriveFont(22f));
        cabecalho.add(titulo, BorderLayout.WEST);

        JPanel sessao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sessao.add(new JLabel("Logado como " + api.usuarioLogado()));
        JButton sair = new JButton("Sair");
        sair.addActionListener(e -> sair());
        sessao.add(sair);
        cabecalho.add(sessao, BorderLayout.EAST);
        cabecalho.add(new JLabel("Selecione um evento e escolha uma área de trabalho."), BorderLayout.SOUTH);
        return cabecalho;
    }

    private JSplitPane criarConteudo() {
        JPanel eventosPainel = new JPanel(new BorderLayout(8, 8));
        eventosPainel.setBorder(BorderFactory.createTitledBorder("Eventos"));
        eventosPainel.add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel acoesEvento = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton novo = new JButton("Novo evento");
        novo.addActionListener(e -> abrirDialogoEvento(null));
        JButton editar = new JButton("Editar rascunho");
        editar.addActionListener(e -> { JSONObject evento = selecionado(); if (evento != null) abrirDialogoEvento(evento); });
        JButton remover = new JButton("Remover rascunho");
        remover.addActionListener(e -> removerSelecionado());
        JButton atualizar = new JButton("Atualizar lista");
        atualizar.addActionListener(e -> carregarEventos());
        acoesEvento.add(novo);
        acoesEvento.add(editar);
        acoesEvento.add(remover);
        acoesEvento.add(atualizar);
        eventosPainel.add(acoesEvento, BorderLayout.SOUTH);

        JPanel operacoes = new JPanel(new BorderLayout(8, 8));
        operacoes.setBorder(BorderFactory.createTitledBorder("Operações do evento selecionado"));
        eventoSelecionado.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 6));
        operacoes.add(eventoSelecionado, BorderLayout.NORTH);

        javax.swing.JTabbedPane areas = new javax.swing.JTabbedPane();
        areas.addTab("Evento e inscrição", criarAreaEvento());
        areas.addTab("Programação e presença", painelAtividades);
        areas.addTab("Questionários e avaliação", criarAreaQuestionarios());
        areas.addTab("Relatórios", painelRelatorios);
        areas.addTab("Papéis de acesso", criarAreaPapeis());
        areas.setEnabledAt(4, "ADMINISTRADOR".equals(api.papelLogado()));
        operacoes.add(areas, BorderLayout.CENTER);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, eventosPainel, operacoes);
        divisao.setResizeWeight(0.48);
        divisao.setContinuousLayout(true);
        return divisao;
    }

    private JPanel criarAreaEvento() {
        JPanel painel = caixaVertical("Publicação e regras");
        painel.add(new JLabel("Edite o evento enquanto estiver em rascunho. A API valida as regras."));
        botao(painel, "Publicar evento", this::publicarSelecionado);
        botao(painel, "Encerrar evento", this::encerrarSelecionado);
        botao(painel, "Regras de inscrição", this::regrasSelecionadas);
        painel.add(javax.swing.Box.createVerticalGlue());
        return painel;
    }

    private JPanel criarAreaQuestionarios() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel topo = new JPanel(new GridLayout(0, 1, 6, 6));
        topo.add(new JLabel("Escolha uma atividade do evento selecionado para consultar ou criar questionários."));
        atividadeQuestionario.setEnabled(false);
        atividadeQuestionario.addActionListener(e -> {
            if (atualizandoComboAtividades) return;
            int indice = atividadeQuestionario.getSelectedIndex();
            if (indice < 0 || indice >= atividadesQuestionarioIds.size()) {
                painelQuestionarios.definirAtividade(0, null);
            } else {
                painelQuestionarios.definirAtividade(atividadesQuestionarioIds.get(indice),
                        (String) atividadeQuestionario.getSelectedItem());
            }
        });
        topo.add(atividadeQuestionario);
        painel.add(topo, BorderLayout.NORTH);
        painel.add(painelQuestionarios, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarAreaPapeis() {
        JPanel painel = caixaVertical("Papéis de acesso");
        painel.add(new JLabel("A alteração de papel é autorizada pela API somente para administrador."));
        botao(painel, "Alterar papel de uma conta", this::alterarPapel);
        painel.add(javax.swing.Box.createVerticalGlue());
        return painel;
    }

    private JPanel caixaVertical(String titulo) {
        JPanel painel = new JPanel();
        painel.setLayout(new javax.swing.BoxLayout(painel, javax.swing.BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createTitledBorder(titulo));
        painel.setBorder(BorderFactory.createCompoundBorder(
                painel.getBorder(), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        return painel;
    }

    private void botao(JPanel painel, String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.setAlignmentX(LEFT_ALIGNMENT);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, botao.getPreferredSize().height + 8));
        botao.addActionListener(e -> acao.run());
        painel.add(botao);
        painel.add(javax.swing.Box.createVerticalStrut(8));
    }

    private void sair() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    private JSONObject selecionado() {
        int linhaVisual = tabela.getSelectedRow();
        if (linhaVisual < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um evento na lista.");
            return null;
        }
        int linha = tabela.convertRowIndexToModel(linhaVisual);
        return eventos.get(linha);
    }

    private void atualizarContexto() {
        int linhaVisual = tabela.getSelectedRow();
        if (linhaVisual < 0) {
            eventoSelecionado.setText("Selecione um evento na lista.");
            painelAtividades.definirEvento(null);
            painelRelatorios.definirEvento(null);
            carregarAtividadesQuestionario(null);
            return;
        }
        JSONObject evento = eventos.get(tabela.convertRowIndexToModel(linhaVisual));
        eventoSelecionado.setText(evento.optString("titulo") + " · "
                + evento.optString("status") + " · " + evento.optString("local", "A definir"));
        painelAtividades.definirEvento(evento);
        painelRelatorios.definirEvento(evento);
        carregarAtividadesQuestionario(evento);
    }

    private void carregarAtividadesQuestionario(JSONObject evento) {
        long chamada = ++sequenciaAtividades;
        atividadeQuestionario.setEnabled(false);
        atividadesQuestionarioIds.clear();
        atividadeQuestionario.removeAllItems();
        painelQuestionarios.definirAtividade(0, null);
        if (evento == null) return;
        TarefaTela.executar(this, () -> api.atividades(evento.getLong("id")), atividades -> {
            if (chamada != sequenciaAtividades) return;
            atualizandoComboAtividades = true;
            for (int i = 0; i < atividades.length(); i++) {
                JSONObject atividade = atividades.getJSONObject(i);
                atividadesQuestionarioIds.add(atividade.getLong("id"));
                atividadeQuestionario.addItem(atividade.getString("titulo"));
            }
            boolean habilitado = !atividadesQuestionarioIds.isEmpty();
            atividadeQuestionario.setEnabled(habilitado);
            if (habilitado) {
                atividadeQuestionario.setSelectedIndex(0);
                painelQuestionarios.definirAtividade(atividadesQuestionarioIds.get(0),
                        (String) atividadeQuestionario.getSelectedItem());
            }
            atualizandoComboAtividades = false;
        });
    }

    private void carregarEventos() {
        TarefaTela.executar(this, api::listar, lista -> {
            Long idAnterior = null;
            int selecionado = tabela.getSelectedRow();
            if (selecionado >= 0 && tabela.convertRowIndexToModel(selecionado) < eventos.size())
                idAnterior = eventos.get(tabela.convertRowIndexToModel(selecionado)).getLong("id");
            eventos.clear();
            eventos.addAll(lista);
            modeloTabela.setRowCount(0);
            int linhaParaSelecionar = -1;
            for (int i = 0; i < lista.size(); i++) {
                JSONObject e = lista.get(i);
                modeloTabela.addRow(new Object[] {
                    e.getString("titulo"), e.getString("modalidade"), e.optString("local", "A definir"),
                    formatarData(e.getString("inicio")), e.getString("status")
                });
                if (idAnterior != null && e.getLong("id") == idAnterior) linhaParaSelecionar = i;
            }
            if (linhaParaSelecionar >= 0) {
                int linhaVisivel = tabela.convertRowIndexToView(linhaParaSelecionar);
                if (linhaVisivel >= 0) tabela.setRowSelectionInterval(linhaVisivel, linhaVisivel);
            }
            else atualizarContexto();
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
            JOptionPane.showMessageDialog(this, "Somente eventos em rascunho podem ser editados.");
            return;
        }
        JTextField titulo = new JTextField(evento == null ? "" : evento.optString("titulo"));
        JTextField descricao = new JTextField(evento == null ? "" : evento.optString("descricao"));
        String dia = java.time.LocalDate.now().plusDays(1).toString();
        JTextField inicio = new JTextField(evento == null ? dia + "T09:00:00" : evento.optString("inicio"));
        JTextField fim = new JTextField(evento == null ? dia + "T18:00:00" : evento.optString("fim"));
        JTextField local = new JTextField(evento == null ? "A definir" : evento.optString("local", "A definir"));
        JTextField fuso = new JTextField(evento == null ? "America/Sao_Paulo" : evento.optString("fuso", "America/Sao_Paulo"));
        JComboBox<String> modalidade = new JComboBox<>(new String[] {"PRESENCIAL", "ONLINE", "HIBRIDO"});
        if (evento != null) {
            modalidade.setSelectedItem(evento.optString("modalidade", "PRESENCIAL"));
            fuso.setEditable(false);
        }

        JPanel painel = new JPanel(new GridLayout(0, 2, 8, 6));
        adicionarCampo(painel, "Título", titulo);
        adicionarCampo(painel, "Descrição", descricao);
        adicionarCampo(painel, "Início (aaaa-mm-ddThh:mm:ss)", inicio);
        adicionarCampo(painel, "Fim (aaaa-mm-ddThh:mm:ss)", fim);
        adicionarCampo(painel, "Local", local);
        adicionarCampo(painel, "Fuso IANA (fixo após criação)", fuso);
        adicionarCampo(painel, "Modalidade", modalidade);
        if (JOptionPane.showConfirmDialog(this, painel, evento == null ? "Novo evento" : "Editar evento",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        JSONObject corpo = new JSONObject().put("titulo", titulo.getText().trim())
                .put("descricao", descricao.getText().trim()).put("inicio", inicio.getText().trim())
                .put("fim", fim.getText().trim()).put("local", local.getText().trim())
                .put("fuso", fuso.getText().trim()).put("modalidade", modalidade.getSelectedItem());
        TarefaTela.executar(this, () -> evento == null ? api.criarEvento(corpo)
                : api.editarEvento(evento.getLong("id"), corpo), resposta -> carregarEventos());
    }

    private void adicionarCampo(JPanel painel, String titulo, java.awt.Component campo) {
        painel.add(new JLabel(titulo));
        painel.add(campo);
    }

    private void removerSelecionado() {
        JSONObject evento = selecionado();
        if (evento == null) return;
        if (!evento.optString("status").equals("RASCUNHO")) {
            JOptionPane.showMessageDialog(this, "Somente rascunhos podem ser removidos.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Remover o rascunho selecionado?", "Confirmar remoção",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        TarefaTela.executar(this, () -> { api.remover(evento.getLong("id")); return true; }, ok -> carregarEventos());
    }

    private void publicarSelecionado() { mudarEstadoEvento("publicar"); }
    private void encerrarSelecionado() { mudarEstadoEvento("encerrar"); }

    private void mudarEstadoEvento(String operacao) {
        JSONObject evento = selecionado();
        if (evento == null) return;
        TarefaTela.executar(this, () -> {
            if (operacao.equals("publicar")) api.publicar(evento.getLong("id"));
            else api.encerrar(evento.getLong("id"));
            return true;
        }, ok -> carregarEventos());
    }

    private void regrasSelecionadas() {
        JSONObject evento = selecionado();
        if (evento == null) return;
        TarefaTela.executar(this, () -> api.regrasInscricao(evento.getLong("id")), regras -> exibirEditorRegras(evento, regras));
    }

    private void exibirEditorRegras(JSONObject evento, JSONObject atuais) {
        javax.swing.JCheckBox escolher = new javax.swing.JCheckBox("Participante escolhe atividades", atuais.getBoolean("escolherAtividades"));
        javax.swing.JCheckBox vagas = new javax.swing.JCheckBox("Controlar capacidade/vagas", atuais.getBoolean("controlarVagas"));
        JTextField prazo = new JTextField(atuais.getString("prazoCancelamento"));
        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(escolher);
        painel.add(vagas);
        painel.add(new JLabel("Prazo (aaaa-mm-ddThh:mm:ss; fuso do evento)"));
        painel.add(prazo);
        painel.add(new JLabel("A API impede mudanças após a primeira inscrição."));
        if (JOptionPane.showConfirmDialog(this, painel, "Regras — " + evento.optString("titulo"),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        JSONObject corpo = new JSONObject().put("escolherAtividades", escolher.isSelected())
                .put("controlarVagas", vagas.isSelected()).put("prazoCancelamento", prazo.getText().trim());
        TarefaTela.executar(this, () -> api.configurarRegrasInscricao(evento.getLong("id"), corpo),
                resposta -> JOptionPane.showMessageDialog(this, "Regras de inscrição salvas."));
    }

    private void alterarPapel() {
        JTextField usuarioId = new JTextField();
        JComboBox<String> papel = new JComboBox<>(new String[] {"PARTICIPANTE", "ORGANIZADOR", "ADMINISTRADOR"});
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 8));
        adicionarCampo(campos, "ID da conta", usuarioId);
        adicionarCampo(campos, "Novo papel", papel);
        campos.add(new JLabel("Use o ID de outra conta; não é permitido alterar a própria conta."));
        campos.add(new JLabel("A API confirma se sua conta pode executar esta ação."));
        if (JOptionPane.showConfirmDialog(this, campos, "Alterar papel", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        try {
            long id = Long.parseLong(usuarioId.getText().trim());
            TarefaTela.executar(this,
                    () -> new JSONObject(api.requisicao("PUT", "/usuarios/" + id + "/papel",
                            new JSONObject().put("papel", papel.getSelectedItem()))),
                    usuario -> JOptionPane.showMessageDialog(this,
                            "Papel atualizado para " + usuario.getString("papel") + "."));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Informe o ID numérico de uma conta existente.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new LoginScreen().setVisible(true);
        });
    }
}
