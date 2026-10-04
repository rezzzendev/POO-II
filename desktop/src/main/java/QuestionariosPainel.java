import org.json.JSONArray;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Painel de criação de questionários e consulta de resultados pela API compartilhada. */
public class QuestionariosPainel extends JPanel {
    private final EventoApiClient api;
    private long atividadeId;
    private long sequenciaCarregamento;
    private final JLabel contexto = new JLabel("Selecione uma atividade para consultar questionários.");
    private final JButton novo = new JButton("Novo questionário");
    private final JButton resultados = new JButton("Ver resultados");
    private final List<JSONObject> questionarios = new ArrayList<>();
    private final DefaultTableModel modelo =
            new DefaultTableModel(new String[] {"ID", "Título", "Perguntas"}, 0) {
                @Override
                public boolean isCellEditable(int linha, int coluna) {
                    return false;
                }
            };
    private final JTable tabela = new JTable(modelo);

    public QuestionariosPainel(EventoApiClient api) {
        super(new BorderLayout());
        this.api = api;
        add(contexto, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        JPanel botoes = new JPanel();
        JButton atualizar = new JButton("Atualizar");
        atualizar.addActionListener(e -> carregar());
        novo.addActionListener(e -> novo());
        resultados.addActionListener(e -> resultados());
        botoes.add(atualizar);
        botoes.add(novo);
        botoes.add(resultados);
        add(botoes, BorderLayout.SOUTH);
        definirAtividade(0, null);
    }

    public void definirAtividade(long atividadeId, String titulo) {
        this.atividadeId = atividadeId;
        boolean habilitado = atividadeId > 0;
        contexto.setText(habilitado ? "Questionários da atividade: " + titulo
                : "Selecione uma atividade para consultar questionários.");
        novo.setEnabled(habilitado);
        resultados.setEnabled(habilitado);
        questionarios.clear();
        modelo.setRowCount(0);
        if (habilitado) carregar();
    }

    private void carregar() {
        if (atividadeId <= 0) return;
        long id = atividadeId;
        long chamada = ++sequenciaCarregamento;
        TarefaTela.executar(
                this,
                () -> api.questionarios(id),
                lista -> {
                    if (chamada != sequenciaCarregamento || atividadeId != id) return;
                    questionarios.clear();
                    modelo.setRowCount(0);
                    for (int i = 0; i < lista.length(); i++) {
                        JSONObject q = lista.getJSONObject(i);
                        questionarios.add(q);
                        modelo.addRow(
                                new Object[] {
                                    q.getLong("id"),
                                    q.getString("titulo"),
                                    q.getJSONArray("perguntas").length()
                                });
                    }
                });
    }

    private void novo() {
        JTextField titulo = new JTextField();
        JTextField quantidade = new JTextField("3");
        JPanel inicio = new JPanel(new GridLayout(0, 1, 4, 4));
        inicio.add(new JLabel("Título do questionário"));
        inicio.add(titulo);
        inicio.add(new JLabel("Quantidade de perguntas (1 a 50)"));
        inicio.add(quantidade);
        if (JOptionPane.showConfirmDialog(
                        this, inicio, "Novo questionário", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        try {
            int total = Integer.parseInt(quantidade.getText().trim());
            if (total < 1 || total > 50) throw new NumberFormatException();
            JSONArray perguntas = new JSONArray();
            for (int i = 0; i < total; i++) {
                JSONObject pergunta = criarPergunta(i + 1);
                if (pergunta == null) return;
                perguntas.put(pergunta);
            }
            JSONObject corpo =
                    new JSONObject()
                            .put("atividadeId", atividadeId)
                            .put("titulo", titulo.getText().trim())
                            .put("perguntas", perguntas);
            TarefaTela.executar(this, () -> api.criarQuestionario(corpo), resposta -> carregar());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Informe uma quantidade entre 1 e 50.");
        }
    }

    private JSONObject criarPergunta(int numero) {
        String enunciado =
                JOptionPane.showInputDialog(this, "Enunciado da pergunta " + numero + ":");
        if (enunciado == null) return null;
        String[] tipos = {"TEXTO", "ESCOLHA_UNICA", "ESCALA"};
        String tipo =
                (String)
                        JOptionPane.showInputDialog(
                                this,
                                "Tipo da pergunta " + numero,
                                "Tipo de resposta",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                tipos,
                                tipos[0]);
        if (tipo == null) return null;
        JSONObject pergunta = new JSONObject().put("enunciado", enunciado).put("tipo", tipo);
        if (tipo.equals("ESCOLHA_UNICA")) {
            String texto =
                    JOptionPane.showInputDialog(
                            this, "Opções separadas por vírgula (ex.: Sim,Não)");
            if (texto == null) return null;
            JSONArray opcoes = new JSONArray();
            for (String opcao : texto.split(","))
                if (!opcao.isBlank()) opcoes.put(opcao.trim());
            pergunta.put("opcoes", opcoes);
        } else if (tipo.equals("ESCALA")) {
            String minimo = JOptionPane.showInputDialog(this, "Valor mínimo da escala", "1");
            if (minimo == null) return null;
            String maximo = JOptionPane.showInputDialog(this, "Valor máximo da escala", "5");
            if (maximo == null) return null;
            pergunta.put("minimo", Integer.parseInt(minimo.trim()));
            pergunta.put("maximo", Integer.parseInt(maximo.trim()));
        }
        return pergunta;
    }

    private void resultados() {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= questionarios.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um questionário.");
            return;
        }
        long id = questionarios.get(linha).getLong("id");
        TarefaTela.executar(
                this,
                () -> api.resultadosQuestionario(id),
                resultado -> mostrarResultados(resultado));
    }

    private void mostrarResultados(JSONObject resultado) {
        StringBuilder texto =
                new StringBuilder("Total de respostas: ")
                        .append(resultado.getInt("total"))
                        .append("\n")
                        .append(resultado.getString("politicaIdentificacao"))
                        .append("\n\n");
        JSONArray perguntas = resultado.getJSONArray("perguntas");
        for (int i = 0; i < perguntas.length(); i++) {
            JSONObject p = perguntas.getJSONObject(i);
            texto.append(p.getString("enunciado")).append('\n');
            if (p.has("media")) texto.append("Média: ").append(p.getDouble("media")).append('\n');
            JSONObject distribuicao = p.getJSONObject("distribuicao");
            for (String chave : distribuicao.keySet())
                texto.append("  ").append(chave).append(": ")
                        .append(distribuicao.getInt(chave)).append('\n');
            JSONArray comentarios = p.getJSONArray("comentarios");
            for (int j = 0; j < comentarios.length(); j++)
                texto.append("  - ").append(comentarios.getString(j)).append('\n');
            texto.append('\n');
        }
        JTextArea area = new JTextArea(texto.toString(), 18, 55);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Resultados", JOptionPane.INFORMATION_MESSAGE);
    }
}
