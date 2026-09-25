package adapter.in.api;

import application.usuario.UsuarioRepository;

import com.sun.net.httpserver.HttpExchange;

import domain.usuario.Papel;
import domain.usuario.Usuario;

/**
 * RNF-06: operação protegida é verificada aqui, no servidor — nunca só escondida na tela do
 * cliente.
 */
public class Autenticador {

    private final SessaoStore sessoes;
    private final UsuarioRepository usuarios;

    public Autenticador(SessaoStore sessoes, UsuarioRepository usuarios) {
        this.sessoes = sessoes;
        this.usuarios = usuarios;
    }

    public Usuario exigir(HttpExchange exchange, Papel... papeisPermitidos) {
        String cabecalho = exchange.getRequestHeaders().getFirst("Authorization");
        String token = cabecalho == null ? null : cabecalho.replaceFirst("^Bearer ", "");
        Long usuarioId = sessoes.usuarioDoToken(token);

        Usuario usuario = usuarioId == null ? null : usuarios.buscarPorId(usuarioId).orElse(null);
        if (usuario == null) {
            throw new NaoAutenticadoException("É preciso estar logado.");
        }
        if (papeisPermitidos.length > 0 && !permiteAcesso(papeisPermitidos, usuario.getPapel())) {
            throw new NaoAutorizadoException("Seu perfil não tem permissão pra essa ação.");
        }
        return usuario;
    }

    public boolean podeGerenciar(HttpExchange exchange) {
        if (exchange.getRequestHeaders().getFirst("Authorization") == null) return false;
        return exigir(exchange).getPapel() != Papel.PARTICIPANTE;
    }

    private boolean permiteAcesso(Papel[] papeisPermitidos, Papel papelDoUsuario) {
        for (Papel papel : papeisPermitidos) {
            if (papel == papelDoUsuario) {
                return true;
            }
        }
        return false;
    }
}
