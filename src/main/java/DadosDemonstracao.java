import adapter.out.persistence.*;
import adapter.out.persistence.atividade.AtividadeRepositoryJdbc;
import adapter.out.persistence.evento.EventoRepositoryJdbc;
import adapter.out.persistence.inscricao.*;
import adapter.out.persistence.usuario.UsuarioRepositoryJdbc;

import domain.atividade.*;
import domain.avaliacao.*;
import domain.evento.*;
import domain.inscricao.*;
import domain.usuario.*;

import java.time.*;
import java.util.*;

/** Opt-in, idempotente. Usa somente pessoas fictícias; não apaga registros existentes. */
public class DadosDemonstracao {
    public static void main(String[] args) throws Exception {
        var usuarios = new UsuarioRepositoryJdbc();
        var eventos = new EventoRepositoryJdbc();
        var atividades = new AtividadeRepositoryJdbc(eventos, usuarios);
        var inscricoes = new InscricaoRepositoryJdbc(usuarios, eventos);
        criarUsuario(usuarios, "Administrador Demo", "admin@demo.local", Papel.ADMINISTRADOR);
        var organizador =
                criarUsuario(
                        usuarios, "Organizador Demo", "organizador@demo.local", Papel.ORGANIZADOR);
        var participante =
                criarUsuario(
                        usuarios,
                        "Participante Demo",
                        "participante@demo.local",
                        Papel.PARTICIPANTE);
        // Um hash de senha pública de demonstração reutilizado apenas nesta carga sintética.
        String hash = participante.getSenhaHash();
        try (var c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try (var p =
                    c.prepareStatement(
                            "INSERT INTO usuarios(nome,email,senha_hash,papel) SELECT ?,?,?,? WHERE"
                                    + " NOT EXISTS (SELECT 1 FROM usuarios WHERE email=?)")) {
                for (int i = 1; i <= 500; i++) {
                    String email = "participante" + i + "@demo.local";
                    p.setString(1, "Participante " + i);
                    p.setString(2, email);
                    p.setString(3, hash);
                    p.setString(4, "PARTICIPANTE");
                    p.setString(5, email);
                    p.addBatch();
                }
                p.executeBatch();
            }
            c.commit();
        }
        Evento evento =
                eventos.listarTodos().stream()
                        .filter(e -> e.getTitulo().equals("JAVA8 — Demonstração"))
                        .findFirst()
                        .orElse(null);
        if (evento == null) {
            var inicio =
                    LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0)
                            .minusHours(1);
            evento =
                    Evento.novo(
                            "JAVA8 — Demonstração",
                            "Base fictícia para os cenários CA-01 a CA-07",
                            inicio,
                            inicio.plusDays(7),
                            Modalidade.PRESENCIAL);
            evento.definirLocalEFuso("Campus UEG", "America/Sao_Paulo");
            evento = eventos.salvar(evento);
        }
        var existentes = atividades.listarPorEvento(evento.getId());
        for (int i = 0; i < 100; i++) {
            String titulo = "Atividade demo " + (i + 1);
            if (existentes.stream().anyMatch(a -> a.getTitulo().equals(titulo))) continue;
            var inicio = evento.getInicio().plusHours(i);
            atividades.salvar(
                    Atividade.nova(
                            titulo,
                            "Atividade para demonstração",
                            i % 2 == 0 ? "Oficina" : "Palestra",
                            "Trilha " + (i % 3 + 1),
                            "Sala " + (i % 10 + 1),
                            inicio,
                            inicio.plusMinutes(50),
                            600,
                            evento));
        }
        var primeira = atividades.listarPorEvento(evento.getId()).getFirst();
        if (atividades.listarPessoas(primeira.getId()).isEmpty())
            atividades.vincularPessoa(
                    primeira.getId(),
                    new VinculoPessoa(organizador.getId(), organizador.getNome(), "Palestrante"));
        if (evento.getStatus() == StatusEvento.RASCUNHO) {
            evento.publicar();
            evento = eventos.salvar(evento);
        }
        if (Sql.listar(
                        "SELECT evento_id FROM regras_inscricao WHERE evento_id=?",
                        r -> r.getLong(1),
                        evento.getId())
                .isEmpty())
            new RegrasInscricaoJdbc()
                    .salvar(evento.getId(), new RegrasInscricao(true, true, evento.getFim()));
        if (!inscricoes.existeInscricaoAtiva(participante.getId(), evento.getId()))
            inscricoes.salvar(Inscricao.nova(participante, evento, List.of(primeira.getId())));
        final long eventoId = evento.getId();
        // 500 inscrições tornam os relatórios representativos, além das 100 atividades.
        for (long id :
                Sql.listar(
                        "SELECT id FROM usuarios WHERE email LIKE 'participante%@demo.local'",
                        r -> r.getLong(1))) {
            if (!inscricoes.existeInscricaoAtiva(id, eventoId))
                inscricoes.salvar(
                        Inscricao.nova(
                                usuarios.buscarPorId(id).orElseThrow(),
                                evento,
                                List.of(primeira.getId())));
        }
        var questionarios = new AvaliacaoRepositoryJdbc();
        if (questionarios.listar(primeira.getId()).isEmpty())
            questionarios.salvar(
                    new Questionario(
                            0,
                            primeira.getId(),
                            "Avaliação da atividade",
                            List.of(
                                    new Pergunta(0, "O que podemos melhorar?", new Texto()),
                                    new Pergunta(
                                            0,
                                            "Recomendaria esta atividade?",
                                            new EscolhaUnica(List.of("Sim", "Não"))),
                                    new Pergunta(0, "Nota geral", new Escala(1, 5)))));
        System.out.println(
                "Base pronta: 500+ participantes, 100 atividades. Senha das contas demo: Demo123!");
        System.out.println("admin@demo.local | organizador@demo.local | participante@demo.local");
    }

    private static Usuario criarUsuario(
            UsuarioRepositoryJdbc repo, String nome, String email, Papel papel) {
        return repo.buscarPorEmail(email)
                .orElseGet(() -> repo.salvar(Usuario.novo(nome, email, "Demo123!", papel)));
    }
}
