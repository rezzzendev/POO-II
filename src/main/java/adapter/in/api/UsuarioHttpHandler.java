package adapter.in.api;

import application.usuario.UsuarioRepository;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import domain.usuario.Papel;
import domain.usuario.Usuario;
import domain.usuario.UsuarioInvalidoException;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Registrado em três contextos ("/usuarios", "/login" e "/usuarios/me") apontando pra esta mesma
 * instância — RF-01 (cadastro), RF-02 (autenticação) e RF-03 (participante consulta/atualiza os
 * próprios dados).
 */
public class UsuarioHttpHandler implements HttpHandler {

    private final application.usuario.UsuarioService service;
    private final UsuarioRepository repository;
    private final SessaoStore sessoes;
    private final Autenticador autenticador;

    public UsuarioHttpHandler(
            UsuarioRepository repository, SessaoStore sessoes, Autenticador autenticador) {
        this.repository = repository;
        this.service = new application.usuario.UsuarioService(repository);
        this.sessoes = sessoes;
        this.autenticador = autenticador;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpJson.tratarPreflight(exchange)) {
            return;
        }

        try {
            String caminho = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();

            if (caminho.equals("/usuarios") && metodo.equals("POST")) {
                cadastrar(exchange);
                return;
            }
            if (caminho.equals("/login") && metodo.equals("POST")) {
                login(exchange);
                return;
            }
            if (caminho.equals("/usuarios/me") && metodo.equals("GET")) {
                meuPerfil(exchange);
                return;
            }
            if (caminho.equals("/usuarios/me") && metodo.equals("PUT")) {
                editarMeuPerfil(exchange);
                return;
            }

            if (caminho.matches("/usuarios/[0-9]+/papel") && metodo.equals("PUT")) {
                var admin = autenticador.exigir(exchange, Papel.ADMINISTRADOR);
                long id = Long.parseLong(caminho.split("/")[2]);
                if (id == admin.getId())
                    throw new UsuarioInvalidoException(
                            "Não altere seu próprio perfil administrativo.");
                var usuario =
                        repository
                                .buscarPorId(id)
                                .orElseThrow(
                                        () ->
                                                new UsuarioInvalidoException(
                                                        "Usuário não encontrado."));
                usuario.alterarPapel(Papel.valueOf(HttpJson.lerCorpo(exchange).getString("papel")));
                HttpJson.responder(exchange, 200, paraJson(repository.salvar(usuario)));
                return;
            }
            HttpJson.responder(exchange, 404, HttpJson.erro("Rota não encontrada."));
        } catch (UsuarioInvalidoException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro(e.getMessage()));
        } catch (NaoAutorizadoException e) {
            HttpJson.responder(exchange, 403, HttpJson.erro(e.getMessage()));
        } catch (NaoAutenticadoException e) {
            HttpJson.responder(exchange, 401, HttpJson.erro(e.getMessage()));
        } catch (IllegalArgumentException
                | org.json.JSONException
                | java.time.DateTimeException e) {
            HttpJson.responder(exchange, 400, HttpJson.erro(e.getMessage()));
        } catch (Exception e) {
            HttpJson.falhaInterna(exchange, e);
        }
    }

    private void cadastrar(HttpExchange exchange) throws IOException {
        JSONObject corpo = HttpJson.lerCorpo(exchange);
        // Papel sempre PARTICIPANTE no autocadastro — ninguém vira
        // ADMINISTRADOR/ORGANIZADOR só porque mandou esse campo no JSON.
        Usuario salvo =
                service.cadastrar(
                        corpo.optString("nome", null),
                        corpo.optString("email", null),
                        corpo.optString("senha", null));
        HttpJson.responder(exchange, 201, paraJson(salvo));
    }

    private void login(HttpExchange exchange) throws IOException {
        JSONObject corpo = HttpJson.lerCorpo(exchange);
        String email = corpo.optString("email", null);
        String senha = corpo.optString("senha", null);

        Usuario usuario = service.autenticar(email, senha);
        if (usuario == null) {
            throw new NaoAutenticadoException("E-mail ou senha inválidos.");
        }

        JSONObject resposta = paraJson(usuario);
        resposta.put("token", sessoes.criar(usuario.getId()));
        HttpJson.responder(exchange, 200, resposta);
    }

    private void meuPerfil(HttpExchange exchange) throws IOException {
        Usuario usuario = autenticador.exigir(exchange);
        HttpJson.responder(exchange, 200, paraJson(usuario));
    }

    private void editarMeuPerfil(HttpExchange exchange) throws IOException {
        Usuario usuario = autenticador.exigir(exchange);
        JSONObject corpo = HttpJson.lerCorpo(exchange);
        HttpJson.responder(
                exchange,
                200,
                paraJson(
                        service.editar(
                                usuario,
                                corpo.optString("nome", null),
                                corpo.optString("email", null))));
    }

    private JSONObject paraJson(Usuario usuario) {
        JSONObject json = new JSONObject();
        json.put("id", usuario.getId());
        json.put("nome", usuario.getNome());
        json.put("email", usuario.getEmail());
        json.put("papel", usuario.getPapel().name());
        return json;
    }
}
