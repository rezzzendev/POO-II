package domain;

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

        if (titulo == null || titulo.isBlank()) {
            throw new EventoInvalidoException("Título do evento é obrigatório.");
        }
        if (modalidade == null) {
            throw new EventoInvalidoException("Modalidade do evento é obrigatória.");
        }
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new EventoInvalidoException("O término do evento deve ser posterior ao início.");
        }

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

    public Long getId(){return id;}
    public String getTitulo(){return titulo;}
    public String getDescricao(){return descricao;}
    public LocalDateTime getInicio(){return inicio;}
    public LocalDateTime getFim(){return fim;}
    public Modalidade getModalidade(){return modalidade;}
    public StatusEvento getStatus(){return status;}

}
