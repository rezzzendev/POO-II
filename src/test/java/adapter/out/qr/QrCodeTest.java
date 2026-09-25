package adapter.out.qr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

class QrCodeTest {
    @Test
    void geraELeCemCodigosComPadroesDiferentes() throws Exception {
        var qr = new QrCode();
        for (int i = 0; i < 100; i++) {
            String token =
                    UUID.nameUUIDFromBytes(("regressao-qr-" + i).getBytes(StandardCharsets.UTF_8))
                            .toString();
            assertEquals(token, qr.ler(qr.gerar(token)), "Token " + token);
        }
    }

    @Test
    void rejeitaArquivoQueNaoEImagem() {
        assertThrows(IllegalArgumentException.class, () -> new QrCode().ler(new byte[] {1, 2, 3}));
    }
}
