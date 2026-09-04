package domain.atividade;

import domain.evento.Evento;
import java.time.LocalDateTime;

public class Atividade {

    private Long id;
    private String titulo;
    private String descricao;
    private String tipo;
    private String trilha;
    private String local;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private Evento evento;

    public Atividade(Long id, String titulo, String descricao, String tipo, String trilha, String local,
                     LocalDateTime inicio, LocalDateTime fim, Evento evento){

        this.id = id;
        this.tipo = titulo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.trilha = trilha;
        this.local = local;
        this.inicio = inicio;
        this.fim = fim;
        this.evento = evento;
    }

    public static Atividade novo(String titulo, String descricao, String tipo, String trilha, String local,
                                 LocalDateTime inicio, LocalDateTime fim, Evento evento){
        return new Atividade(null, titulo, descricao, tipo, trilha, local, inicio, fim, evento);
    }

    public Long getId(){return id;}
    public String getTitulo(){return titulo;}
    public String getDescricao(){return descricao;}
    public String getTipo(){return  tipo;}
    public String getTrilha(){return trilha;}
    public String getLocal(){return local;}
    public LocalDateTime getInicio(){return inicio;}
    public LocalDateTime getFim(){return fim;}
    public Evento getEvento(){return evento;}

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof Atividade)) return false;
        Atividade atividade = (Atividade) o;
        return id != null && id.equals(atividade.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString(){
        return "Atividade{"+
                "id=" + id +
                "titulo=" + titulo +
                "descricao=" + descricao +
                "tipo=" + tipo +
                "}";
    }

}
