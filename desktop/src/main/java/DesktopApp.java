import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Tela de trabalho do organizador. Toda operação é enviada para a API. */
public class DesktopApp extends JFrame {

    private final EventoApiClient api;
    private final List<JSONObject> eventos = new ArrayList<>();
    private final JComboBox<String> seletorEventos = new JComboBox<>();
    private final JLabel resumoEvento = new JLabel("Selecione um evento.");
    private final AtividadesPainel atividades;
    private final QuestionariosPainel questionarios;
    private final RelatoriosPainel relatorios;
    private final JComboBox<String> atividadeQuestionario = new JComboBox<>();
    private final List<Long> atividadesQuestionarioIds = new ArrayList<>();
    private boolean atualizandoEventos;
    private boolean atualizandoAtividades;
    private long sequenciaAtividades;

    public DesktopApp(EventoApiClient api) {
        super("Gestão de Eventos");
        this.api = api;
        atividades = new AtividadesPainel(api);
        questionarios = new QuestionariosPainel(api);
        relatorios = new RelatoriosPainel(api);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1080, 720);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBackground(Tema.FUNDO);
        raiz.add(criarCabecalho(), BorderLayout.NORTH);
        JPanel margem = new JPanel(new BorderLayout());
        margem.setBackground(Tema.FUNDO);
        margem.setBorder(BorderFactory.createEmptyBorder(2, 16, 16, 16));
        margem.add(criarConteudo(), BorderLayout.CENTER);
        raiz.add(margem, BorderLayout.CENTER);
        add(raiz);

        seletorEventos.addActionListener(e -> {
            if (!atualizandoEventos) atualizarContexto();
        });
        carregarEventos();
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(18, 4));
        cabecalho.setBackground(Tema.AZUL);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel identidade = new JPanel();
        identidade.setOpaque(false);
        identidade.setLayout(new BoxLayout(identidade, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Gestão de eventos");
        titulo.setForeground(Tema.BRANCO);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));
        identidade.add(titulo);
        JLabel instrucao = new JLabel("Administração e organização · siga as quatro etapas");
        instrucao.setForeground(new java.awt.Color(0xD4, 0xE4, 0xEC));
        instrucao.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        identidade.add(instrucao);
        cabecalho.add(identidade, BorderLayout.WEST);

        JPanel sessao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sessao.setOpaque(false);
        JLabel usuario = new JLabel(api.usuarioLogado());
        usuario.setForeground(Tema.BRANCO);
        sessao.add(usuario);
        if ("ADMINISTRADOR".equals(api.papelLogado())) {
            JButton usuarios = new JButton("Gerenciar usuários");
            Tema.botaoSecundario(usuarios);
            usuarios.addActionListener(e -> alterarPapel());
            sessao.add(usuarios);
        }
        JButton sair = new JButton("Sair");
        Tema.botaoSecundario(sair);
        sair.addActionListener(e -> sair());
        sessao.add(sair);
        cabecalho.add(sessao, BorderLayout.EAST);
        return cabecalho;
    }

    private JPanel criarConteudo() {
        JPanel conteudo = new JPanel(new BorderLayout(10, 10));

        JPanel escolha = new JPanel(new BorderLayout(10, 6));
        Tema.aplicarCartao(escolha);
        JLabel rotuloEvento = new JLabel("Evento em gestão");
        rotuloEvento.setFont(rotuloEvento.getFont().deriveFont(Font.BOLD));
        escolha.add(rotuloEvento, BorderLayout.WEST);
        escolha.add(seletorEventos, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton atualizar = new JButton("Atualizar");
        Tema.botaoSecundario(atualizar);
        atualizar.addActionListener(e -> carregarEventos());
        JButton novo = new JButton("Novo evento");
        Tema.botaoPrimario(novo);
        novo.addActionListener(e -> abrirDialogoEvento(null));
        JButton editar = new JButton("Editar evento");
        Tema.botaoSecundario(editar);
        editar.addActionListener(e -> {
            JSONObject evento = selecionado();
            if (evento != null) abrirDialogoEvento(evento);
        });
        JButton excluir = new JButton("Excluir rascunho");
        Tema.botaoPerigo(excluir);
        excluir.addActionListener(e -> removerSelecionado());
        acoes.add(atualizar);
        acoes.add(novo);
        acoes.add(editar);
        acoes.add(excluir);
        acoes.setOpaque(false);
        escolha.add(acoes, BorderLayout.EAST);
        resumoEvento.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        escolha.add(resumoEvento, BorderLayout.SOUTH);
        conteudo.add(escolha, BorderLayout.NORTH);

        JTabbedPane etapas = new JTabbedPane();
        etapas.setBackground(Tema.FUNDO);
        etapas.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        etapas.addTab("1. Evento", criarAreaEvento());
        etapas.addTab("2. Programação e frequência", atividades);
        etapas.addTab("3. Avaliações", criarAreaQuestionarios());
        etapas.addTab("4. Relatórios", relatorios);
        conteudo.add(etapas, BorderLayout.CENTER);
        return conteudo;
    }

    private JPanel criarAreaEvento() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JLabel titulo = new JLabel("Prepare o evento antes de publicá-lo");
        titulo.setFont(titulo.getFont().deriveFont(18f));
        titulo.setAlignmentX(LEFT_ALIGNMENT);
        painel.add(titulo);
        painel.add(Box.createVerticalStrut(8));
        JLabel ajuda = new JLabel("Defina a inscrição, monte a programação e só então publique.");
        ajuda.setAlignmentX(LEFT_ALIGNMENT);
        painel.add(ajuda);
        painel.add(Box.createVerticalStrut(18));
        adicionarBotao(painel, "Configurar inscrição", this::regrasSelecionadas);
        adicionarBotao(painel, "Publicar evento", this::publicarSelecionado);
        adicionarBotao(painel, "Encerrar evento", this::encerrarSelecionado);
        painel.add(Box.createVerticalGlue());
        return painel;
    }

    private JPanel criarAreaQuestionarios() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topo = new JPanel(new BorderLayout(8, 4));
        topo.add(new JLabel("Atividade"), BorderLayout.WEST);
        atividadeQuestionario.setEnabled(false);
        atividadeQuestionario.addActionListener(e -> {
            if (atualizandoAtividades) return;
            int indice = atividadeQuestionario.getSelectedIndex();
            if (indice < 0 || indice >= atividadesQuestionarioIds.size()) {
                questionarios.definirAtividade(0, null);
            } else {
                questionarios.definirAtividade(
                        atividadesQuestionarioIds.get(indice),
                        (String) atividadeQuestionario.getSelectedItem());
            }
        });
        topo.add(atividadeQuestionario, BorderLayout.CENTER);
        painel.add(topo, BorderLayout.NORTH);
        painel.add(questionarios, BorderLayout.CENTER);
        return painel;
    }

    private void adicionarBotao(JPanel painel, String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        Tema.botaoPrimario(botao);
        botao.setAlignmentX(LEFT_ALIGNMENT);
        botao.setMaximumSize(new Dimension(280, botao.getPreferredSize().height + 8));
        botao.addActionListener(e -> acao.run());
        painel.add(botao);
        painel.add(Box.createVerticalStrut(10));
    }

    private void sair() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }

    private JSONObject selecionadoSemAviso() {
        int indice = seletorEventos.getSelectedIndex();
        return indice >= 0 && indice < eventos.size() ? eventos.get(indice) : null;
    }

    private JSONObject selecionado() {
        JSONObject evento = selecionadoSemAviso();
        if (evento == null) JOptionPane.showMessageDialog(this, "Selecione um evento.");
        return evento;
    }

    private void atualizarContexto() {
        JSONObject evento = selecionadoSemAviso();
        if (evento == null) {
            resumoEvento.setText("Nenhum evento selecionado.");
            atividades.definirEvento(null);
            relatorios.definirEvento(null);
            carregarAtividadesQuestionario(null);
            return;
        }
        resumoEvento.setText(
                evento.optString("status") + "  |  " + evento.optString("modalidade")
                        + "  |  " + evento.optString("local", "Local a definir")
                        + "  |  " + formatarData(evento.optString("inicio")));
        atividades.definirEvento(evento);
        relatorios.definirEvento(evento);
        carregarAtividadesQuestionario(evento);
    }

    private void carregarAtividadesQuestionario(JSONObject evento) {
        long chamada = ++sequenciaAtividades;
        atualizandoAtividades = true;
        atividadeQuestionario.setEnabled(false);
        atividadesQuestionarioIds.clear();
        atividadeQuestionario.removeAllItems();
        questionarios.definirAtividade(0, null);
        atualizandoAtividades = false;
        if (evento == null) return;

        TarefaTela.executar(this, () -> api.atividades(evento.getLong("id")), lista -> {
            if (chamada != sequenciaAtividades) return;
            atualizandoAtividades = true;
            for (int i = 0; i < lista.length(); i++) {
                JSONObject atividade = lista.getJSONObject(i);
                atividadesQuestionarioIds.add(atividade.getLong("id"));
                atividadeQuestionario.addItem(atividade.getString("titulo"));
            }
            atividadeQuestionario.setEnabled(!atividadesQuestionarioIds.isEmpty());
            if (!atividadesQuestionarioIds.isEmpty()) {
                atividadeQuestionario.setSelectedIndex(0);
                questionarios.definirAtividade(
                        atividadesQuestionarioIds.get(0),
                        (String) atividadeQuestionario.getSelectedItem());
            }
            atualizandoAtividades = false;
        });
    }

    private void carregarEventos() {
        JSONObject atual = selecionadoSemAviso();
        Long idAnterior = atual == null ? null : atual.getLong("id");
        TarefaTela.executar(this, api::listar, lista -> {
            atualizandoEventos = true;
            eventos.clear();
            eventos.addAll(lista);
            seletorEventos.removeAllItems();
            int selecionar = -1;
            for (int i = 0; i < eventos.size(); i++) {
                JSONObject evento = eventos.get(i);
                seletorEventos.addItem(
                        evento.getString("titulo") + " — " + evento.getString("status"));
                if (idAnterior != null && evento.getLong("id") == idAnterior) selecionar = i;
            }
            if (selecionar < 0 && !eventos.isEmpty()) selecionar = 0;
            seletorEventos.setSelectedIndex(selecionar);
            atualizandoEventos = false;
            atualizarContexto();
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
        if (evento != null && !"RASCUNHO".equals(evento.optString("status"))) {
            JOptionPane.showMessageDialog(this, "Somente eventos em rascunho podem ser editados.");
            return;
        }
        String dia = java.time.LocalDate.now().plusDays(1).toString();
        JTextField titulo = new JTextField(evento == null ? "" : evento.optString("titulo"));
        JTextField descricao = new JTextField(evento == null ? "" : evento.optString("descricao"));
        JTextField inicio = new JTextField(
                evento == null ? dia + "T09:00:00" : evento.optString("inicio"));
        JTextField fim = new JTextField(
                evento == null ? dia + "T18:00:00" : evento.optString("fim"));
        JTextField local = new JTextField(
                evento == null ? "A definir" : evento.optString("local"));
        JTextField fuso = new JTextField(
                evento == null ? "America/Sao_Paulo" : evento.optString("fuso"));
        JComboBox<String> modalidade = new JComboBox<>(
                new String[] {"PRESENCIAL", "ONLINE", "HIBRIDO"});
        if (evento != null) {
            modalidade.setSelectedItem(evento.optString("modalidade", "PRESENCIAL"));
            fuso.setEditable(false);
        }

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 6));
        adicionarCampo(formulario, "Título", titulo);
        adicionarCampo(formulario, "Descrição", descricao);
        adicionarCampo(formulario, "Início (aaaa-mm-ddThh:mm:ss)", inicio);
        adicionarCampo(formulario, "Fim (aaaa-mm-ddThh:mm:ss)", fim);
        adicionarCampo(formulario, "Local", local);
        adicionarCampo(formulario, "Fuso do evento", fuso);
        adicionarCampo(formulario, "Modalidade", modalidade);
        if (JOptionPane.showConfirmDialog(
                        this, formulario, evento == null ? "Novo evento" : "Editar evento",
                        JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        JSONObject corpo = new JSONObject()
                .put("titulo", titulo.getText().trim())
                .put("descricao", descricao.getText().trim())
                .put("inicio", inicio.getText().trim())
                .put("fim", fim.getText().trim())
                .put("local", local.getText().trim())
                .put("fuso", fuso.getText().trim())
                .put("modalidade", modalidade.getSelectedItem());
        TarefaTela.executar(
                this,
                () -> evento == null
                        ? api.criarEvento(corpo)
                        : api.editarEvento(evento.getLong("id"), corpo),
                resposta -> carregarEventos());
    }

    private void adicionarCampo(JPanel painel, String titulo, java.awt.Component campo) {
        painel.add(new JLabel(titulo));
        painel.add(campo);
    }

    private void publicarSelecionado() {
        mudarEstadoEvento(true);
    }

    private void removerSelecionado() {
        JSONObject evento = selecionado();
        if (evento == null) return;
        if (!"RASCUNHO".equals(evento.optString("status"))) {
            JOptionPane.showMessageDialog(this, "Somente rascunhos podem ser excluídos.");
            return;
        }
        if (JOptionPane.showConfirmDialog(
                        this, "Excluir o evento em rascunho?", "Confirmar exclusão",
                        JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        TarefaTela.executar(this, () -> {
            api.removerEvento(evento.getLong("id"));
            return true;
        }, ok -> carregarEventos());
    }

    private void encerrarSelecionado() {
        mudarEstadoEvento(false);
    }

    private void mudarEstadoEvento(boolean publicar) {
        JSONObject evento = selecionado();
        if (evento == null) return;
        TarefaTela.executar(this, () -> {
            if (publicar) api.publicar(evento.getLong("id"));
            else api.encerrar(evento.getLong("id"));
            return true;
        }, ok -> carregarEventos());
    }

    private void regrasSelecionadas() {
        JSONObject evento = selecionado();
        if (evento == null) return;
        TarefaTela.executar(
                this,
                () -> api.regrasInscricao(evento.getLong("id")),
                regras -> exibirEditorRegras(evento, regras));
    }

    private void exibirEditorRegras(JSONObject evento, JSONObject atuais) {
        javax.swing.JCheckBox escolher = new javax.swing.JCheckBox(
                "Participante escolhe atividades", atuais.getBoolean("escolherAtividades"));
        javax.swing.JCheckBox vagas = new javax.swing.JCheckBox(
                "Controlar capacidade das atividades", atuais.getBoolean("controlarVagas"));
        JTextField prazo = new JTextField(atuais.getString("prazoCancelamento"));
        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.add(escolher);
        painel.add(vagas);
        painel.add(new JLabel("Prazo para cancelamento (aaaa-mm-ddThh:mm:ss)"));
        painel.add(prazo);
        if (JOptionPane.showConfirmDialog(
                        this, painel, "Inscrição — " + evento.optString("titulo"),
                        JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        JSONObject corpo = new JSONObject()
                .put("escolherAtividades", escolher.isSelected())
                .put("controlarVagas", vagas.isSelected())
                .put("prazoCancelamento", prazo.getText().trim());
        TarefaTela.executar(
                this,
                () -> api.configurarRegrasInscricao(evento.getLong("id"), corpo),
                resposta -> JOptionPane.showMessageDialog(this, "Regras de inscrição salvas."));
    }

    private void alterarPapel() {
        JTextField email = new JTextField();
        JComboBox<String> papel = new JComboBox<>(
                new String[] {"PARTICIPANTE", "ORGANIZADOR", "ADMINISTRADOR"});
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 8));
        adicionarCampo(campos, "E-mail da conta", email);
        adicionarCampo(campos, "Novo papel", papel);
        if (JOptionPane.showConfirmDialog(
                        this, campos, "Gerenciar usuário", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        TarefaTela.executar(
                this,
                () -> api.alterarPapel(
                        email.getText().trim(), (String) papel.getSelectedItem()),
                usuario -> JOptionPane.showMessageDialog(
                        this,
                        usuario.getString("nome")
                                + " agora é "
                                + usuario.getString("papel")
                                + "."));
    }
}
