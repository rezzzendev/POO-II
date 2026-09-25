package adapter.in.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessões em memória — sem JWT, sem lib externa. Reiniciar a API derruba todo mundo logado;
 * aceitável pro escopo do projeto (login social/MFA estão explicitamente fora do escopo
 * obrigatório).
 */
public class SessaoStore {

    private final Map<String, Long> usuarioIdPorToken = new ConcurrentHashMap<>();

    public String criar(Long usuarioId) {
        String token = UUID.randomUUID().toString();
        usuarioIdPorToken.put(token, usuarioId);
        return token;
    }

    public Long usuarioDoToken(String token) {
        return token == null ? null : usuarioIdPorToken.get(token);
    }
}
