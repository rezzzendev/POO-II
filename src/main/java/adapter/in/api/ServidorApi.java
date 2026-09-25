package adapter.in.api;

import adapter.out.persistence.*;
import adapter.out.persistence.atividade.AtividadeRepositoryJdbc;
import adapter.out.persistence.evento.EventoRepositoryJdbc;
import adapter.out.persistence.inscricao.*;
import adapter.out.persistence.usuario.UsuarioRepositoryJdbc;

import application.avaliacao.AvaliacaoService;
import application.frequencia.FrequenciaService;
import application.inscricao.InscricaoService;
import application.relatorio.RelatorioService;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;

/** Composição explícita das dependências. Sem contêiner de injeção ou framework. */
public final class ServidorApi {
    public static HttpServer criar(int porta) throws IOException {
        return criar(porta, Clock.systemUTC());
    }

    public static HttpServer criar(int porta, Clock relogio) throws IOException {
        var eventos = new EventoRepositoryJdbc();
        var usuarios = new UsuarioRepositoryJdbc();
        var atividades = new AtividadeRepositoryJdbc(eventos, usuarios);
        var inscricoes = new InscricaoRepositoryJdbc(usuarios, eventos);
        var regras = new RegrasInscricaoJdbc();
        var inscricaoService =
                new InscricaoService(inscricoes, eventos, atividades, regras, relogio);
        var frequenciaService =
                new FrequenciaService(
                        new FrequenciaRepositoryJdbc(), atividades, inscricoes, regras, relogio);
        var avaliacaoService =
                new AvaliacaoService(new AvaliacaoRepositoryJdbc(), frequenciaService, relogio);
        var relatorios =
                new RelatorioService(inscricoes, atividades, eventos, regras, frequenciaService);
        var sessoes = new SessaoStore();
        var auth = new Autenticador(sessoes, usuarios);
        var usuarioHandler = new UsuarioHttpHandler(usuarios, sessoes, auth);
        var servidor =
                HttpServer.create(
                        new InetSocketAddress(System.getProperty("api.host", "127.0.0.1"), porta),
                        0);
        servidor.createContext("/eventos", new EventoHttpHandler(eventos, auth, atividades));
        servidor.createContext(
                "/atividades", new AtividadeHttpHandler(atividades, eventos, usuarios, auth));
        servidor.createContext(
                "/inscricoes",
                new InscricaoHttpHandler(inscricoes, eventos, atividades, auth, inscricaoService));
        servidor.createContext("/regras-inscricao", new RegrasHttpHandler(inscricaoService, auth));
        servidor.createContext("/agenda", new AgendaHttpHandler(inscricoes, atividades, auth));
        servidor.createContext("/usuarios", usuarioHandler);
        servidor.createContext("/login", usuarioHandler);
        servidor.createContext("/frequencia", new FrequenciaHttpHandler(frequenciaService, auth));
        servidor.createContext("/questionarios", new AvaliacaoHttpHandler(avaliacaoService, auth));
        servidor.createContext("/relatorios", new RelatorioHttpHandler(relatorios, auth));
        servidor.createContext("/", new SiteHttpHandler());
        // Um processo, um executor sequencial: validação + escrita sem corrida entre requisições.
        servidor.setExecutor(null);
        return servidor;
    }
}
