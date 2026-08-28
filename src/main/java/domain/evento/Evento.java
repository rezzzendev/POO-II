package domain.evento;

import java.time.LocalDateTime;
/*
    Entidade Eventos
    Autor: Matheus Rezende
    Data inicio: 22/08/2026
    Relacionado: EventoInvalidoException, Modalidade e StatusEvento

*/
public class Evento {

    private Long id;
    private String titulo;
    private String descricao;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private Modalidade modalidade;
    private StatusEvento status;

    public Evento(Long id, String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim, Modalidade modalidade, StatusEvento status){
        validar(titulo, inicio, fim, modalidade);

        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim  = fim;
        this.modalidade = modalidade;
        this.status = status;
    }

    public static Evento novo(String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim, Modalidade modalidade){
        return new Evento(null, titulo, descricao, inicio, fim, modalidade, StatusEvento.RASCUNHO);
    }

    public void editar(String titulo, String descricao, LocalDateTime inicio, LocalDateTime fim, Modalidade modalidade){
        if (status != StatusEvento.RASCUNHO) {
            throw new EventoInvalidoException("Só é possível editar um evento em rascunho.");
        }
        validar(titulo, inicio, fim, modalidade);

        this.titulo = titulo;
        this.descricao = descricao;
        this.inicio = inicio;
        this.fim = fim;
        this.modalidade = modalidade;
    }

    public void publicar(){
        if (status != StatusEvento.RASCUNHO) {
            throw new EventoInvalidoException("Só é possível publicar um evento em rascunho.");
        }
        this.status = StatusEvento.PUBLICADO;
    }

    public void encerrar(){
        if (status != StatusEvento.PUBLICADO) {
            throw new EventoInvalidoException("Só é possível encerrar um evento publicado.");
        }
        this.status = StatusEvento.ENCERRADO;
    }

    private static void validar(String titulo, LocalDateTime inicio, LocalDateTime fim, Modalidade modalidade){
        if (titulo == null || titulo.isBlank()) {
            throw new EventoInvalidoException("Título do evento é obrigatório.");
        }
        if (modalidade == null) {
            throw new EventoInvalidoException("Modalidade do evento é obrigatória.");
        }
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new EventoInvalidoException("O término do evento deve ser posterior ao início.");
        }
    }

    public Long getId(){return id;}
    public String getTitulo(){return titulo;}
    public String getDescricao(){return descricao;}
    public LocalDateTime getInicio(){return inicio;}
    public LocalDateTime getFim(){return fim;}
    public Modalidade getModalidade(){return modalidade;}
    public StatusEvento getStatus(){return status;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Evento)) return false;
        Evento evento = (Evento) o;
        return id != null && id.equals(evento.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Evento{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", modalidade=" + modalidade +
                ", status=" + status +
                '}';
    }
}
