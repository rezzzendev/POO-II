package domain.usuario;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 puro (javax.crypto do JDK, sem lib externa) — RNF-05: senha nunca em texto puro. Só o
 * Usuario fala com essa classe; o resto do sistema nem sabe que ela existe.
 */
class SenhaHasher {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_CHAVE_BITS = 256;

    static String hash(String senha) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        byte[] chave = derivar(senha, sal);
        return Base64.getEncoder().encodeToString(sal)
                + ":"
                + Base64.getEncoder().encodeToString(chave);
    }

    static boolean confere(String senha, String hashArmazenado) {
        String[] partes = hashArmazenado.split(":");
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        byte[] chaveEsperada = Base64.getDecoder().decode(partes[1]);
        byte[] chaveCalculada = derivar(senha, sal);
        return MessageDigest.isEqual(chaveEsperada, chaveCalculada);
    }

    private static byte[] derivar(String senha, byte[] sal) {
        try {
            PBEKeySpec especificacao =
                    new PBEKeySpec(senha.toCharArray(), sal, ITERACOES, TAMANHO_CHAVE_BITS);
            return SecretKeyFactory.getInstance(ALGORITMO)
                    .generateSecret(especificacao)
                    .getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Falha ao gerar hash de senha.", e);
        }
    }
}
