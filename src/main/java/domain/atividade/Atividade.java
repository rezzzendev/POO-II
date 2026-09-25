package domain.atividade;

import domain.evento.Evento;

import java.time.LocalDateTime;

/*
    Entidade Atividade
    Relacionado: AtividadeInvalidaException, Evento (RN-03: atividade não
    existe sem um evento dono), VinculoPessoa (RF-08)

    "tipo" é String, não enum — RF-05 pede "palestra, apresentação oral,
    pôster, produto, mesa, oficina ou outro tipo configurado", ou seja, uma
    lista aberta, não fechada.

    "capacidade" nula = vagas ilimitadas (RF-13/RF-14: controle de vagas é
    configurável, não obrigatório).
*/
public class Atividade {

    private Long id;
    private String titulo;
    private String descricao;
    private String tipo;
    private String trilha;
    private String local;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private Integer capacidade;
    private Evento evento;

    public Atividade(
            Long id,
            String titulo,
            String descricao,
            String tipo,
            String trilha,
            String local,
            LocalDateTime inicio,
            LocalDateTime fim,
            Integer capacidade,
            Evento evento) {
        validarTextos(descricao, trilha);
        validar(titulo, tipo, local, inicio, fim, capacidade, evento);

        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.trilha = trilha;
        this.local = local;
        this.inicio = inicio;
        this.fim = fim;
        this.capacidade = capacidade;
        this.evento = evento;
    }

    public static Atividade nova(
            String titulo,
            String descricao,
            String tipo,
            String trilha,
            String local,
            LocalDateTime inicio,
            LocalDateTime fim,
            Integer capacidade,
            Evento evento) {
        return new Atividade(
                null, titulo, descricao, tipo, trilha, local, inicio, fim, capacidade, evento);
    }

    public void editar(
            String titulo,
            String descricao,
            String tipo,
            String trilha,
            String local,
            LocalDateTime inicio,
            LocalDateTime fim,
            Integer capacidade) {
        validarTextos(descricao, trilha);
        validar(titulo, tipo, local, inicio, fim, capacidade, this.evento);

        this.titulo = titulo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.trilha = trilha;
        this.local = local;
        this.inicio = inicio;
        this.fim = fim;
        this.capacidade = capacidade;
    }

    /**
     * Decisão registrada: back-to-back (uma termina exatamente quando a outra começa) NÃO conta
     * como sobreposição.
     */
    public boolean sobrepoeHorario(Atividade outra) {
        return this.inicio.isBefore(outra.fim) && outra.inicio.isBefore(this.fim);
    }

    /** RF-07/RN-07: conflito de programação = mesmo local + horário sobreposto. */
    public boolean conflitaCom(Atividade outra) {
        boolean mesmoLocal = this.local != null && this.local.equalsIgnoreCase(outra.local);
        return mesmoLocal && sobrepoeHorario(outra);
    }

    /** RF-14: vagas controladas só quando a atividade tem capacidade definida. */
    public boolean temVagaDisponivel(int inscritosConfirmados) {
        return capacidade == null || inscritosConfirmados < capacidade;
    }

    private static void validarTextos(String descricao, String trilha) {
        if (descricao != null && descricao.length() > 2000)
            throw new AtividadeInvalidaException("Descrição deve ter até 2000 caracteres.");
        if (trilha != null && trilha.length() > 100)
            throw new AtividadeInvalidaException("Trilha deve ter até 100 caracteres.");
    }

    private static void validar(
            String titulo,
            String tipo,
            String local,
            LocalDateTime inicio,
            LocalDateTime fim,
            Integer capacidade,
            Evento evento) {
        if (titulo == null || titulo.isBlank() || titulo.length() > 255) {
            throw new AtividadeInvalidaException("Título da atividade é obrigatório.");
        }
        if (tipo == null || tipo.isBlank() || tipo.length() > 100) {
            throw new AtividadeInvalidaException("Tipo da atividade é obrigatório.");
        }
        if (local == null || local.isBlank() || local.length() > 255) {
            throw new AtividadeInvalidaException("Local da atividade é obrigatório.");
        }
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new AtividadeInvalidaException(
                    "O término da atividade deve ser posterior ao início.");
        }
        if (capacidade != null && capacidade <= 0) {
            throw new AtividadeInvalidaException(
                    "Capacidade, quando informada, precisa ser maior que zero.");
        }
        if (evento == null) {
            throw new AtividadeInvalidaException("Atividade precisa pertencer a um evento.");
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTrilha() {
        return trilha;
    }

    public String getLocal() {
        return local;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public Evento getEvento() {
        return evento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Atividade)) return false;
        Atividade atividade = (Atividade) o;
        return id != null && id.equals(atividade.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Atividade{"
                + "id="
                + id
                + ", titulo='"
                + titulo
                + '\''
                + ", tipo='"
                + tipo
                + '\''
                + ", local='"
                + local
                + '\''
                + ", evento="
                + (evento == null ? null : evento.getId())
                + '}';
    }
}
