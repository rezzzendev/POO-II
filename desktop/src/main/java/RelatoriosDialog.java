import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Consulta e exporta os relatórios administrativos oferecidos pela API. */
public class RelatoriosDialog extends JDialog {
    private final EventoApiClient api;
    private final JSONObject evento;
    private final JComboBox<String> tipo = new JComboBox<>(new String[] {"Inscritos", "Frequência"});
    private final JTextField atividadeId = new JTextField(8);
    private final JLabel resumo = new JLabel("Informe filtros e selecione Consultar.");
    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new String[] {"Conta", "Nome", "E-mail", "Inscrição", "Atividade", "Presente", "Marcações"},
                    0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };

    public RelatoriosDialog(JFrame pai, EventoApiClient api, JSONObject evento) {
        super(pai, "Relatórios — " + evento.getString("titulo"), false);
        this.api = api;
        this.evento = evento;
        setSize(950, 480);
        setLocationRelativeTo(pai);
        setLayout(new BorderLayout(8, 8));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEADING));
        filtros.add(new JLabel("Relatório"));
        filtros.add(tipo);
        filtros.add(new JLabel("ID da atividade (opcional)"));
        filtros.add(atividadeId);
        JButton consultar = new JButton("Consultar");
        consultar.addActionListener(e -> consultar());
        JButton exportar = new JButton("Exportar CSV");
        exportar.addActionListener(e -> exportar());
        filtros.add(consultar);
        filtros.add(exportar);
        add(filtros, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        add(resumo, BorderLayout.SOUTH);
    }

    private String rota() {
        return tipo.getSelectedIndex() == 0 ? "inscritos" : "frequencia";
    }

    private String consulta() {
        String filtro = atividadeId.getText().trim();
        if (!filtro.isEmpty()) Long.parseLong(filtro);
        return "/relatorios/" + rota() + "?eventoId=" + evento.getLong("id")
                + (filtro.isEmpty() ? "" : "&atividadeId=" + filtro);
    }

    private void consultar() {
        try {
            String caminho = consulta();
            TarefaTela.executar(
                    this,
                    () -> new JSONObject(api.requisicao("GET", caminho, null)),
                    this::mostrar);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "O ID da atividade deve ser um número inteiro.");
        }
    }

    private void mostrar(JSONObject relatorio) {
        JSONArray linhas = relatorio.getJSONArray("linhas");
        modelo.setRowCount(0);
        for (int i = 0; i < linhas.length(); i++) {
            JSONObject linha = linhas.getJSONObject(i);
            modelo.addRow(
                    new Object[] {
                        linha.getLong("usuarioId"),
                        linha.getString("nome"),
                        linha.getString("email"),
                        linha.getString("inscricao"),
                        linha.isNull("atividade") ? "Evento inteiro" : linha.getString("atividade"),
                        linha.getBoolean("presente") ? "Sim" : "Não",
                        linha.getInt("marcacoes")
                    });
        }
        resumo.setText(
                "Total: " + relatorio.getInt("total")
                        + " · Inscrições confirmadas: " + relatorio.getInt("confirmados")
                        + " · Presentes: " + relatorio.getInt("presentes"));
    }

    private void exportar() {
        final String caminho;
        try {
            caminho = consulta() + "&formato=csv";
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "O ID da atividade deve ser um número inteiro.");
            return;
        }
        JFileChooser escolha = new JFileChooser();
        escolha.setSelectedFile(new File("relatorio-" + rota() + ".csv"));
        if (escolha.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.nio.file.Path destino = escolha.getSelectedFile().toPath();
        TarefaTela.executar(
                this,
                () -> {
                    String csv = api.requisicao("GET", caminho, null);
                    java.nio.file.Files.writeString(
                            destino, csv, StandardCharsets.UTF_8);
                    return true;
                },
                salvo -> JOptionPane.showMessageDialog(this, "Relatório CSV salvo com sucesso."));
    }
}
