import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Painel de consulta e exportação dos relatórios administrativos oferecidos pela API. */
public class RelatoriosPainel extends JPanel {
    private final EventoApiClient api;
    private JSONObject evento;
    private final JButton consultar;
    private final JButton exportar;
    private final JLabel contexto;
    private final JComboBox<String> tipo = new JComboBox<>(new String[] {"Inscritos", "Frequência"});
    private final JComboBox<String> atividade = new JComboBox<>();
    private final List<Long> atividadeIds = new ArrayList<>();
    private final JLabel resumo = new JLabel("Informe filtros e selecione Consultar.");
    private long sequenciaConsulta;
    private long sequenciaAtividades;
    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new String[] {"Conta", "Nome", "E-mail", "Inscrição", "Atividade", "Presente", "Marcações"},
                    0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };

    public RelatoriosPainel(EventoApiClient api) {
        super(new BorderLayout(8, 8));
        this.api = api;
        this.contexto = new JLabel("Selecione um evento para consultar os relatórios.");
        this.consultar = new JButton("Consultar");
        this.exportar = new JButton("Exportar CSV");
        this.evento = null;

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEADING));
        filtros.add(new JLabel("Relatório"));
        filtros.add(tipo);
        filtros.add(new JLabel("Atividade"));
        filtros.add(atividade);
        consultar.addActionListener(e -> consultar());
        exportar.addActionListener(e -> exportar());
        filtros.add(consultar);
        filtros.add(exportar);
        add(filtros, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(modelo)), BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout(8, 8));
        rodape.add(contexto, BorderLayout.NORTH);
        rodape.add(resumo, BorderLayout.SOUTH);
        add(rodape, BorderLayout.SOUTH);
        definirEvento(null);
    }

    public void definirEvento(JSONObject evento) {
        this.evento = evento;
        sequenciaConsulta++;
        boolean habilitado = evento != null;
        consultar.setEnabled(habilitado);
        exportar.setEnabled(habilitado);
        contexto.setText(habilitado ? "Relatórios do evento: " + evento.optString("titulo")
                : "Selecione um evento para consultar os relatórios.");
        modelo.setRowCount(0);
        resumo.setText("Selecione Consultar para carregar os dados.");
        carregarAtividades();
    }

    private String rota() {
        return tipo.getSelectedIndex() == 0 ? "inscritos" : "frequencia";
    }

    private String consulta() {
        if (evento == null) throw new IllegalStateException("Selecione um evento primeiro.");
        int indice = atividade.getSelectedIndex();
        Long filtro = indice <= 0 || indice > atividadeIds.size()
                ? null : atividadeIds.get(indice - 1);
        return "/relatorios/" + rota() + "?eventoId=" + evento.getLong("id")
                + (filtro == null ? "" : "&atividadeId=" + filtro);
    }

    private void carregarAtividades() {
        atividade.removeAllItems();
        atividade.addItem("Evento inteiro");
        atividadeIds.clear();
        if (evento == null) {
            atividade.setEnabled(false);
            return;
        }
        long eventoId = evento.getLong("id");
        long chamada = ++sequenciaAtividades;
        atividade.setEnabled(false);
        TarefaTela.executar(this, () -> api.atividades(eventoId), lista -> {
            if (chamada != sequenciaAtividades || evento == null
                    || evento.getLong("id") != eventoId) return;
            for (int i = 0; i < lista.length(); i++) {
                JSONObject item = lista.getJSONObject(i);
                atividadeIds.add(item.getLong("id"));
                atividade.addItem(item.getString("titulo"));
            }
            atividade.setEnabled(true);
        });
    }

    private void consultar() {
        String caminho = consulta();
        long chamada = ++sequenciaConsulta;
        TarefaTela.executar(
                this,
                () -> new JSONObject(api.requisicao("GET", caminho, null)),
                resultado -> {
                    if (chamada == sequenciaConsulta) mostrar(resultado);
                });
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
        final String caminho = consulta() + "&formato=csv";
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
