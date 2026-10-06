import org.json.*;

import java.awt.*;
import java.util.Base64;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Programação e frequência da atividade selecionada. */
public class AtividadesPainel extends JPanel {
    private final EventoApiClient api;
    private JSONObject evento;
    private final JLabel contexto = new JLabel("Selecione um evento para carregar a programação.");
    private final java.util.List<JButton> botoesComEvento = new java.util.ArrayList<>();
    private long sequenciaCarregamento;
    private final DefaultTableModel modelo =
            new DefaultTableModel(new String[] {"ID", "Título", "Tipo", "Local", "Início"}, 0) {
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };
    private final JTable tabela = new JTable(modelo);

    public AtividadesPainel(EventoApiClient api) {
        super(new BorderLayout(8, 8));
        this.api = api;

        JPanel topo = new JPanel(new BorderLayout(8, 4));
        topo.add(contexto, BorderLayout.CENTER);
        JPanel programacao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botao(programacao, "Nova atividade", this::nova);
        botao(programacao, "Editar atividade", this::editar);
        botao(programacao, "Excluir atividade", this::remover);
        topo.add(programacao, BorderLayout.EAST);
        add(topo, BorderLayout.NORTH);

        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel operacoes = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel pessoas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pessoas.setBorder(BorderFactory.createTitledBorder("Pessoas da atividade"));
        botao(pessoas, "Consultar e vincular", this::pessoas);
        operacoes.add(pessoas);

        JPanel frequencia = new JPanel(new FlowLayout(FlowLayout.LEFT));
        frequencia.setBorder(BorderFactory.createTitledBorder("Frequência"));
        botao(frequencia, "Definir critério", this::politica);
        botao(frequencia, "Gerar QR Code", this::qr);
        botao(frequencia, "Lançar manualmente", this::manual);
        operacoes.add(frequencia);
        add(operacoes, BorderLayout.SOUTH);
        definirEvento(null);
    }

    public void definirEvento(JSONObject evento) {
        this.evento = evento;
        boolean habilitado = evento != null;
        contexto.setText(habilitado
                ? "Programação: " + evento.optString("titulo") + " · " + evento.optString("status")
                : "Selecione um evento para carregar a programação.");
        botoesComEvento.forEach(botao -> botao.setEnabled(habilitado));
        modelo.setRowCount(0);
        if (habilitado) carregar();
    }

    private void botao(JPanel painel, String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> acao.run());
        painel.add(b);
        botoesComEvento.add(b);
    }

    private Long selecionada() {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma atividade.");
            return null;
        }
        return ((Number) modelo.getValueAt(linha, 0)).longValue();
    }

    private void carregar() {
        if (evento == null) return;
        long eventoId = evento.getLong("id");
        long chamada = ++sequenciaCarregamento;
        TarefaTela.executar(
                this,
                () -> api.atividades(eventoId),
                lista -> {
                    if (chamada != sequenciaCarregamento
                            || evento == null
                            || evento.getLong("id") != eventoId) return;
                    modelo.setRowCount(0);
                    for (Object o : lista) {
                        JSONObject a = (JSONObject) o;
                        modelo.addRow(
                                new Object[] {
                                    a.getLong("id"),
                                    a.getString("titulo"),
                                    a.getString("tipo"),
                                    a.getString("local"),
                                    formatarData(a.getString("inicio"))
                                });
                    }
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

    private void nova() {
        editarFormulario(null);
    }

    private void editar() {
        Long id = selecionada();
        if (id == null) return;
        if (!evento.getString("status").equals("RASCUNHO")) {
            JOptionPane.showMessageDialog(this, "Somente atividades de eventos em rascunho podem ser editadas.");
            return;
        }
        TarefaTela.executar(
                this,
                () -> new JSONObject(api.requisicao("GET", "/atividades/" + id, null)),
                this::editarFormulario);
    }

    private void editarFormulario(JSONObject atividade) {
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        String[] nomes = {
            "Título",
            "Descrição",
            "Tipo",
            "Trilha",
            "Local",
            "Início (aaaa-mm-ddThh:mm)",
            "Fim (aaaa-mm-ddThh:mm)",
            "Capacidade (vazio = ilimitada)"
        };
        JTextField[] campos = new JTextField[nomes.length];
        for (int i = 0; i < nomes.length; i++) {
            form.add(new JLabel(nomes[i]));
            campos[i] = new JTextField();
            form.add(campos[i]);
        }
        campos[2].setText("Oficina");
        campos[5].setText(evento.getString("inicio"));
        campos[6].setText(evento.getString("fim"));
        if (atividade != null) {
            campos[0].setText(atividade.optString("titulo"));
            campos[1].setText(atividade.optString("descricao"));
            campos[2].setText(atividade.optString("tipo"));
            campos[3].setText(atividade.optString("trilha"));
            campos[4].setText(atividade.optString("local"));
            campos[5].setText(atividade.optString("inicio"));
            campos[6].setText(atividade.optString("fim"));
            if (!atividade.isNull("capacidade"))
                campos[7].setText(String.valueOf(atividade.getInt("capacidade")));
        }
        if (JOptionPane.showConfirmDialog(
                        this,
                        form,
                        atividade == null ? "Nova atividade" : "Editar atividade",
                        JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        try {
            JSONObject b =
                    new JSONObject()
                            .put("eventoId", evento.getLong("id"))
                            .put("titulo", campos[0].getText())
                            .put("descricao", campos[1].getText())
                            .put("tipo", campos[2].getText())
                            .put("trilha", campos[3].getText())
                            .put("local", campos[4].getText())
                            .put("inicio", campos[5].getText())
                            .put("fim", campos[6].getText());
            if (!campos[7].getText().isBlank()) {
                int capacidade = Integer.parseInt(campos[7].getText().trim());
                if (capacidade < 1) throw new NumberFormatException();
                b.put("capacidade", capacidade);
            } else if (atividade != null) {
                b.put("capacidade", JSONObject.NULL);
            }
            TarefaTela.executar(
                    this,
                    () ->
                            atividade == null
                                    ? api.criarAtividade(b)
                                    : api.editarAtividade(atividade.getLong("id"), b),
                    ok -> carregar());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Capacidade deve ser um inteiro positivo ou ficar vazia.");
        }
    }

    private void remover() {
        Long id = selecionada();
        if (id == null) return;
        if (!"RASCUNHO".equals(evento.optString("status"))) {
            JOptionPane.showMessageDialog(
                    this, "Somente atividades de eventos em rascunho podem ser excluídas.");
            return;
        }
        if (JOptionPane.showConfirmDialog(
                        this, "Excluir a atividade selecionada?", "Confirmar exclusão",
                        JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        TarefaTela.executar(this, () -> {
            api.removerAtividade(id);
            return true;
        }, ok -> carregar());
    }

    private void pessoas() {
        Long id = selecionada();
        if (id == null) return;
        String tituloAtividade = String.valueOf(tabela.getValueAt(tabela.getSelectedRow(), 1));
        TarefaTela.executar(
                this,
                () -> api.pessoas(id),
                lista -> {
                    StringBuilder texto = new StringBuilder("Pessoas já vinculadas:\n");
                    for (int i = 0; i < lista.length(); i++) {
                        JSONObject pessoa = lista.getJSONObject(i);
                        texto.append("• ")
                                .append(pessoa.getString("nomePessoa"))
                                .append(" — ")
                                .append(pessoa.getString("papel"))
                                .append("\n");
                    }
                    if (lista.isEmpty()) texto.append("Nenhuma pessoa vinculada.\n");
                    texto.append("\nPara vincular, informe o e-mail usado na conta.");
                    if (JOptionPane.showConfirmDialog(
                                    this,
                                    texto.toString(),
                                    "Pessoas — " + tituloAtividade,
                                    JOptionPane.OK_CANCEL_OPTION)
                            != JOptionPane.OK_OPTION) return;
                    JTextField email = new JTextField();
                    JComboBox<String> papel =
                            new JComboBox<>(new String[] {"PALESTRANTE", "APRESENTADOR", "RESPONSAVEL"});
                    JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
                    formulario.add(new JLabel("E-mail da conta"));
                    formulario.add(email);
                    formulario.add(new JLabel("Papel na atividade"));
                    formulario.add(papel);
                    if (JOptionPane.showConfirmDialog(
                                    this, formulario, "Vincular pessoa", JOptionPane.OK_CANCEL_OPTION)
                            != JOptionPane.OK_OPTION) return;
                    TarefaTela.executar(
                            this,
                            () -> api.vincularPessoa(
                                    id, email.getText().trim(), (String) papel.getSelectedItem()),
                            vinculada -> JOptionPane.showMessageDialog(
                                    this,
                                    vinculada.getString("nomePessoa") + " foi vinculada à atividade."));
                });
    }

    private void politica() {
        Long id = selecionada();
        if (id == null) return;
        String valor =
                (String)
                        JOptionPane.showInputDialog(
                                this,
                                "Critério de presença",
                                "Frequência",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                new String[] {"CHECK_IN", "ENTRADA_SAIDA", "MANUAL"},
                                "CHECK_IN");
        if (valor != null)
            TarefaTela.executar(
                    this,
                    () ->
                            api.requisicao(
                                    "PUT",
                                    "/frequencia/" + id + "/politica",
                                    new JSONObject().put("politica", valor)),
                    ok -> JOptionPane.showMessageDialog(this, "Política salva."));
    }

    private void qr() {
        Long id = selecionada();
        if (id == null) return;
        TarefaTela.executar(this, () -> api.politicaFrequencia(id), politica -> gerarQr(id, politica));
    }

    private void gerarQr(long id, String politica) {
        if ("MANUAL".equals(politica)) {
            JOptionPane.showMessageDialog(
                    this, "Esta atividade usa validação manual e não gera QR Code.");
            return;
        }
        String[] tipos = "ENTRADA_SAIDA".equals(politica)
                ? new String[] {"ENTRADA", "SAIDA"}
                : new String[] {"CHECK_IN"};
        String tipo =
                (String)
                        JOptionPane.showInputDialog(
                                this,
                                "Marcação",
                                "Gerar QR Code",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                tipos,
                                tipos[0]);
        if (tipo == null) return;
        TarefaTela.executar(
                this,
                () -> api.gerarQr(id, tipo),
                codigo -> {
                    byte[] png = Base64.getDecoder().decode(codigo.getString("imagemBase64"));
                    JPanel painel = new JPanel(new BorderLayout());
                    painel.add(new JLabel(new ImageIcon(png)), BorderLayout.CENTER);
                    painel.add(
                            new JLabel("Válido até " + codigo.getString("validade")),
                            BorderLayout.NORTH);
                    JButton salvar = new JButton("Salvar imagem");
                    salvar.addActionListener(
                            e -> {
                                JFileChooser c = new JFileChooser();
                                c.setSelectedFile(new java.io.File("presenca.png"));
                                if (c.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
                                    TarefaTela.executar(
                                            this,
                                            () -> {
                                                java.nio.file.Files.write(
                                                        c.getSelectedFile().toPath(), png);
                                                return true;
                                            },
                                            ok ->
                                                    JOptionPane.showMessageDialog(
                                                            this, "Imagem salva."));
                            });
                    painel.add(salvar, BorderLayout.SOUTH);
                    JOptionPane.showMessageDialog(
                            this, painel, "QR Code de frequência", JOptionPane.PLAIN_MESSAGE);
                });
    }

    private void manual() {
        Long id = selecionada();
        if (id == null) return;
        // Seleção por nome e situação; não exige que o operador conheça IDs internos.
        TarefaTela.executar(
                this,
                () ->
                        new JSONObject(
                                        api.requisicao(
                                                "GET",
                                                "/relatorios/inscritos?eventoId="
                                                        + evento.getLong("id")
                                                        + "&atividadeId="
                                                        + id,
                                                null))
                                .getJSONArray("linhas"),
                linhas -> {
                    JComboBox<String> pessoas = new JComboBox<>();
                    for (Object o : linhas) {
                        JSONObject p = (JSONObject) o;
                        pessoas.addItem(
                                p.getString("nome")
                                        + " — "
                                        + p.getString("email")
                                        + " ("
                                        + p.getString("inscricao")
                                        + ")");
                    }
                    if (linhas.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Nenhum inscrito nesta atividade.");
                        return;
                    }
                    JCheckBox presente = new JCheckBox("Presença validada", true);
                    JTextField motivo = new JTextField();
                    JPanel form = new JPanel(new GridLayout(0, 1));
                    form.add(pessoas);
                    form.add(presente);
                    form.add(new JLabel("Justificativa da conferência ou correção"));
                    form.add(motivo);
                    if (JOptionPane.showConfirmDialog(
                                    this, form, "Lançamento manual", JOptionPane.OK_CANCEL_OPTION)
                            == JOptionPane.OK_OPTION) {
                        long u =
                                linhas.getJSONObject(pessoas.getSelectedIndex())
                                        .getLong("usuarioId");
                        TarefaTela.executar(
                                this,
                                () -> {
                                    api.registrarManual(
                                            id, u, presente.isSelected(), motivo.getText());
                                    return true;
                                },
                                ok ->
                                        JOptionPane.showMessageDialog(
                                                this, "Presença registrada com autoria."));
                    }
                });
    }

}
