import org.json.*;

import java.awt.*;
import java.util.Base64;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Base de administração. Habny pode acrescentar telas usando EventoApiClient e TarefaTela. */
public class AtividadesDialog extends JDialog {
    private final EventoApiClient api;
    private final JSONObject evento;
    private final DefaultTableModel modelo =
            new DefaultTableModel(new String[] {"ID", "Título", "Tipo", "Local", "Início"}, 0) {
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };
    private final JTable tabela = new JTable(modelo);

    public AtividadesDialog(JFrame pai, EventoApiClient api, JSONObject evento) {
        super(pai, "Atividades — " + evento.getString("titulo"), false);
        this.api = api;
        this.evento = evento;
        setSize(900, 450);
        setLocationRelativeTo(pai);
        setLayout(new BorderLayout());
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        JPanel botoes = new JPanel();
        botao(botoes, "Atualizar", this::carregar);
        botao(botoes, "Nova atividade", this::nova);
        botao(botoes, "Editar atividade", this::editar);
        botao(botoes, "Remover atividade", this::remover);
        botao(botoes, "Pessoas vinculadas", this::pessoas);
        botao(botoes, "Política de frequência", this::politica);
        botao(botoes, "Gerar QR Code", this::qr);
        botao(botoes, "Presença manual", this::manual);
        botao(botoes, "Questionários", this::questionarios);
        add(botoes, BorderLayout.SOUTH);
        carregar();
    }

    private void botao(JPanel painel, String texto, Runnable acao) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> acao.run());
        painel.add(b);
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
        TarefaTela.executar(
                this,
                () -> api.atividades(evento.getLong("id")),
                lista -> {
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
        if (!evento.getString("status").equals("RASCUNHO")) {
            JOptionPane.showMessageDialog(this, "Somente atividades de eventos em rascunho podem ser removidas.");
            return;
        }
        int resposta =
                JOptionPane.showConfirmDialog(
                        this,
                        "Remover a atividade selecionada? Só é permitido em evento rascunho.",
                        "Confirmar remoção",
                        JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) return;
        TarefaTela.executar(this, () -> { api.removerAtividade(id); return true; }, ok -> carregar());
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
                                .append(" (conta #")
                                .append(pessoa.getLong("usuarioId"))
                                .append(")\n");
                    }
                    if (lista.isEmpty()) texto.append("Nenhuma pessoa vinculada.\n");
                    texto.append("\nPara vincular, é necessário informar o ID de uma conta existente.");
                    if (JOptionPane.showConfirmDialog(
                                    this,
                                    texto.toString(),
                                    "Pessoas — " + tituloAtividade,
                                    JOptionPane.OK_CANCEL_OPTION)
                            != JOptionPane.OK_OPTION) return;
                    JTextField usuarioId = new JTextField();
                    JComboBox<String> papel =
                            new JComboBox<>(new String[] {"PALESTRANTE", "APRESENTADOR", "RESPONSAVEL"});
                    JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
                    formulario.add(new JLabel("ID da conta existente"));
                    formulario.add(usuarioId);
                    formulario.add(new JLabel("Papel na atividade"));
                    formulario.add(papel);
                    if (JOptionPane.showConfirmDialog(
                                    this, formulario, "Vincular pessoa", JOptionPane.OK_CANCEL_OPTION)
                            != JOptionPane.OK_OPTION) return;
                    try {
                        long conta = Long.parseLong(usuarioId.getText().trim());
                        TarefaTela.executar(
                                this,
                                () -> api.vincularPessoa(id, conta, (String) papel.getSelectedItem()),
                                vinculada -> JOptionPane.showMessageDialog(
                                        this,
                                        vinculada.getString("nomePessoa") + " foi vinculada à atividade."));
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "Informe o ID numérico de uma conta existente.");
                    }
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
        String tipo =
                (String)
                        JOptionPane.showInputDialog(
                                this,
                                "Marcação",
                                "Gerar QR Code",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                new String[] {"CHECK_IN", "ENTRADA", "SAIDA"},
                                "CHECK_IN");
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

    private void questionarios() {
        Long id = selecionada();
        if (id != null) new QuestionariosDialog((JFrame) getOwner(), api, id).setVisible(true);
    }
}
