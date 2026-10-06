import adapter.out.persistence.ConnectionFactory;
import adapter.out.persistence.Sql;
import adapter.out.persistence.atividade.AtividadeRepositoryJdbc;
import adapter.out.persistence.avaliacao.AvaliacaoRepositoryJdbc;
import adapter.out.persistence.evento.EventoRepositoryJdbc;
import adapter.out.persistence.inscricao.RegrasInscricaoJdbc;
import adapter.out.persistence.usuario.UsuarioRepositoryJdbc;

import domain.atividade.Atividade;
import domain.atividade.VinculoPessoa;
import domain.avaliacao.EscolhaUnica;
import domain.avaliacao.Escala;
import domain.avaliacao.Pergunta;
import domain.avaliacao.Questionario;
import domain.avaliacao.Texto;
import domain.evento.Evento;
import domain.evento.Modalidade;
import domain.evento.StatusEvento;
import domain.inscricao.RegrasInscricao;
import domain.usuario.Papel;
import domain.usuario.Usuario;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.util.List;

/** Carga fictícia, idempotente e limitada ao volume mínimo exigido na especificação. */
public class DadosDemonstracao {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int TOTAL_PARTICIPANTES = 500;
    private static final int TOTAL_ATIVIDADES = 100;
    private static final int ATIVIDADES_POR_EVENTO = 10;

    private record EventoDemo(
            String titulo, String descricao, String local, Modalidade modalidade) {}

    private static final List<EventoDemo> EVENTOS =
            List.of(
                    new EventoDemo(
                            "Simpósio JAVA8",
                            "Tecnologia, arquitetura e desenvolvimento de software.",
                            "Campus UEG",
                            Modalidade.PRESENCIAL),
                    new EventoDemo(
                            "Jornada de Inovação",
                            "Oficinas e debates sobre inovação aplicada.",
                            "Centro de Convenções",
                            Modalidade.HIBRIDO),
                    new EventoDemo(
                            "Encontro de Pesquisa",
                            "Apresentações de projetos e resultados acadêmicos.",
                            "Auditório Virtual",
                            Modalidade.ONLINE),
                    new EventoDemo(
                            "Semana de Extensão",
                            "Atividades abertas à comunidade e troca de experiências.",
                            "Campus Central",
                            Modalidade.PRESENCIAL),
                    new EventoDemo(
                            "Congresso de Tecnologia",
                            "Discussões sobre tendências e soluções tecnológicas.",
                            "Centro Tecnológico",
                            Modalidade.HIBRIDO),
                    new EventoDemo(
                            "Mostra de Projetos",
                            "Exposição de projetos desenvolvidos pela comunidade acadêmica.",
                            "Pavilhão de Exposições",
                            Modalidade.PRESENCIAL),
                    new EventoDemo(
                            "Fórum de Empreendedorismo",
                            "Experiências e práticas para transformar ideias em negócios.",
                            "Auditório Principal",
                            Modalidade.HIBRIDO),
                    new EventoDemo(
                            "Conferência de Dados",
                            "Conteúdos sobre análise de dados e inteligência artificial.",
                            "Plataforma on-line",
                            Modalidade.ONLINE),
                    new EventoDemo(
                            "Festival de Ciências",
                            "Divulgação científica com atividades para diferentes públicos.",
                            "Praça Universitária",
                            Modalidade.PRESENCIAL),
                    new EventoDemo(
                            "Seminário de Educação",
                            "Debates sobre práticas e tecnologias aplicadas à educação.",
                            "Bloco de Educação",
                            Modalidade.HIBRIDO));

    private static final String[] TIPOS = {
        "Palestra", "Oficina", "Mesa-redonda", "Apresentação oral", "Pôster"
    };
    private static final String[] TRILHAS = {
        "Desenvolvimento", "Dados", "Inovação", "Pesquisa", "Comunidade"
    };

    public static void main(String[] args) throws Exception {
        var usuarios = new UsuarioRepositoryJdbc();
        var eventos = new EventoRepositoryJdbc();
        var atividades = new AtividadeRepositoryJdbc(eventos, usuarios);
        var regras = new RegrasInscricaoJdbc();

        Usuario administrador =
                criarUsuario(
                        usuarios, "Administrador Demo", "admin@demo.local", Papel.ADMINISTRADOR);
        Usuario organizador =
                criarUsuario(
                        usuarios, "Organizador Demo", "organizador@demo.local", Papel.ORGANIZADOR);
        Usuario participante =
                criarUsuario(
                        usuarios,
                        "Participante Demo",
                        "participante@demo.local",
                        Papel.PARTICIPANTE);

        migrarCargaAntiga();
        criarParticipantes(usuarios, participante);

        LocalDateTime base =
                LocalDateTime.now(FUSO).withMinute(0).withSecond(0).withNano(0).minusHours(1);
        List<Evento> eventosDemo = criarEventos(eventos, atividades, organizador, regras, base);
        Evento primeiroEvento = eventosDemo.getFirst();
        Atividade primeiraAtividade = atividades.listarPorEvento(primeiroEvento.getId()).getFirst();

        inscreverParticipantes(primeiroEvento, primeiraAtividade);
        criarQuestionario(primeiraAtividade);

        long participantesCriados =
                Sql.listar(
                                "SELECT id FROM usuarios WHERE email='participante@demo.local'"
                                        + " OR email LIKE 'participante%@demo.local'",
                                resultado -> resultado.getLong(1))
                        .size();
        long atividadesCriadas =
                eventosDemo.stream()
                        .mapToLong(evento -> atividades.listarPorEvento(evento.getId()).size())
                        .sum();
        if (participantesCriados != TOTAL_PARTICIPANTES
                || atividadesCriadas != TOTAL_ATIVIDADES)
            throw new IllegalStateException(
                    "Carga demo inconsistente: "
                            + participantesCriados
                            + " participantes e "
                            + atividadesCriadas
                            + " atividades.");

        System.out.println(
                "Base pronta: "
                        + EVENTOS.size()
                        + " eventos, 500 participantes e 100 atividades."
                        + " Senha das contas demo: Demo123!");
        System.out.println(
                administrador.getEmail()
                        + " | "
                        + organizador.getEmail()
                        + " | "
                        + participante.getEmail());
    }

    private static void migrarCargaAntiga() {
        // Remove somente registros sintéticos da versão anterior da carga.
        Sql.executar("DELETE FROM eventos WHERE titulo=?", "JAVA8 — Demonstração");
        Sql.executar("DELETE FROM usuarios WHERE email=?", "participante500@demo.local");
    }

    private static void criarParticipantes(UsuarioRepositoryJdbc usuarios, Usuario modelo)
            throws Exception {
        String hash = modelo.getSenhaHash();
        try (var conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try (var comando =
                    conexao.prepareStatement(
                            "INSERT INTO usuarios(nome,email,senha_hash,papel) SELECT ?,?,?,? WHERE"
                                    + " NOT EXISTS (SELECT 1 FROM usuarios WHERE email=?)")) {
                // A conta participante@demo.local + 499 contas = exatamente 500 participantes.
                for (int numero = 1; numero < TOTAL_PARTICIPANTES; numero++) {
                    String email = "participante" + numero + "@demo.local";
                    comando.setString(1, "Participante " + numero);
                    comando.setString(2, email);
                    comando.setString(3, hash);
                    comando.setString(4, Papel.PARTICIPANTE.name());
                    comando.setString(5, email);
                    comando.addBatch();
                }
                comando.executeBatch();
            }
            conexao.commit();
        }
    }

    private static List<Evento> criarEventos(
            EventoRepositoryJdbc eventos,
            AtividadeRepositoryJdbc atividades,
            Usuario organizador,
            RegrasInscricaoJdbc regras,
            LocalDateTime base) {
        var existentes = eventos.listarTodos();
        var criados = new java.util.ArrayList<Evento>();
        for (int indice = 0; indice < EVENTOS.size(); indice++) {
            EventoDemo definicao = EVENTOS.get(indice);
            Evento evento =
                    existentes.stream()
                            .filter(item -> item.getTitulo().equals(definicao.titulo()))
                            .findFirst()
                            .orElse(null);
            if (evento == null) {
                LocalDateTime inicio = base.plusDays(indice * 4L);
                evento =
                        Evento.novo(
                                definicao.titulo(),
                                definicao.descricao(),
                                inicio,
                                inicio.plusDays(2),
                                definicao.modalidade());
                evento.definirLocalEFuso(definicao.local(), FUSO.getId());
                evento = eventos.salvar(evento);
            }

            criarAtividades(atividades, evento, organizador, indice);
            if (evento.getStatus() == StatusEvento.RASCUNHO) {
                evento.publicar();
                evento = eventos.salvar(evento);
            }
            if (Sql.listar(
                            "SELECT evento_id FROM regras_inscricao WHERE evento_id=?",
                            resultado -> resultado.getLong(1),
                            evento.getId())
                    .isEmpty())
                regras.salvar(
                        evento.getId(), new RegrasInscricao(true, true, evento.getFim()));
            criados.add(evento);
        }
        return List.copyOf(criados);
    }

    private static void criarAtividades(
            AtividadeRepositoryJdbc atividades,
            Evento evento,
            Usuario organizador,
            int numeroEvento) {
        String descricaoDemo = "Atividade de demonstração do evento " + evento.getTitulo() + ".";
        long quantidadeDemo =
                Sql.listar(
                                "SELECT id FROM atividades WHERE evento_id=? AND descricao=?",
                                resultado -> resultado.getLong(1),
                                evento.getId(),
                                descricaoDemo)
                        .size();
        if (quantidadeDemo != 0 && quantidadeDemo != ATIVIDADES_POR_EVENTO)
            Sql.executar(
                    "DELETE FROM atividades WHERE evento_id=? AND descricao=?",
                    evento.getId(),
                    descricaoDemo);

        var existentes = atividades.listarPorEvento(evento.getId());
        for (int indice = 0; indice < ATIVIDADES_POR_EVENTO; indice++) {
            String tipo = TIPOS[indice % TIPOS.length];
            String titulo = tipo + " " + (indice + 1) + " — " + TRILHAS[indice % TRILHAS.length];
            if (existentes.stream().anyMatch(item -> item.getTitulo().equals(titulo))) continue;

            int horario = indice / 5;
            int sala = indice % 5 + 1;
            LocalDateTime inicio = evento.getInicio().plusMinutes(30).plusHours(horario * 2L);
            atividades.salvar(
                    Atividade.nova(
                            titulo,
                            descricaoDemo,
                            tipo,
                            TRILHAS[indice % TRILHAS.length],
                            numeroEvento == 2 ? "Sala virtual " + sala : "Sala " + sala,
                            inicio,
                            inicio.plusMinutes(90),
                            TOTAL_PARTICIPANTES,
                            evento));
        }

        Atividade primeira = atividades.listarPorEvento(evento.getId()).getFirst();
        if (atividades.listarPessoas(primeira.getId()).isEmpty())
            atividades.vincularPessoa(
                    primeira.getId(),
                    new VinculoPessoa(
                            organizador.getId(), organizador.getNome(), "PALESTRANTE"));
    }

    private static void inscreverParticipantes(Evento evento, Atividade atividade)
            throws Exception {
        String contasDemo =
                "(u.email='participante@demo.local' OR u.email LIKE 'participante%@demo.local')";
        try (var conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try (var inscricoes =
                    conexao.prepareStatement(
                            "INSERT INTO inscricoes(usuario_id,evento_id,status,data_inscricao) "
                                    + "SELECT u.id,?,'CONFIRMADA',? FROM usuarios u WHERE "
                                    + contasDemo
                                    + " AND NOT EXISTS (SELECT 1 FROM inscricoes i WHERE"
                                    + " i.usuario_id=u.id AND i.evento_id=? AND"
                                    + " i.status='CONFIRMADA')")) {
                inscricoes.setLong(1, evento.getId());
                inscricoes.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now(FUSO)));
                inscricoes.setLong(3, evento.getId());
                inscricoes.executeUpdate();
            }
            try (var escolhas =
                    conexao.prepareStatement(
                            "INSERT INTO inscricao_atividades(inscricao_id,atividade_id) "
                                    + "SELECT i.id,? FROM inscricoes i JOIN usuarios u ON"
                                    + " u.id=i.usuario_id WHERE i.evento_id=? AND"
                                    + " i.status='CONFIRMADA' AND "
                                    + contasDemo
                                    + " AND NOT EXISTS (SELECT 1 FROM inscricao_atividades ia"
                                    + " WHERE ia.inscricao_id=i.id AND ia.atividade_id=?)")) {
                escolhas.setLong(1, atividade.getId());
                escolhas.setLong(2, evento.getId());
                escolhas.setLong(3, atividade.getId());
                escolhas.executeUpdate();
            }
            conexao.commit();
        }
    }

    private static void criarQuestionario(Atividade atividade) {
        var questionarios = new AvaliacaoRepositoryJdbc();
        if (!questionarios.listar(atividade.getId()).isEmpty()) return;
        questionarios.salvar(
                new Questionario(
                        0,
                        atividade.getId(),
                        "Avaliação da atividade",
                        List.of(
                                new Pergunta(0, "O que podemos melhorar?", new Texto()),
                                new Pergunta(
                                        0,
                                        "Recomendaria esta atividade?",
                                        new EscolhaUnica(List.of("Sim", "Não"))),
                                new Pergunta(0, "Nota geral", new Escala(1, 5)))));
    }

    private static Usuario criarUsuario(
            UsuarioRepositoryJdbc repositorio, String nome, String email, Papel papel) {
        return repositorio.buscarPorEmail(email)
                .orElseGet(
                        () -> repositorio.salvar(Usuario.novo(nome, email, "Demo123!", papel)));
    }
}
