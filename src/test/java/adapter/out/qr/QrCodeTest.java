package adapter.out.qr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QrCodeTest {
    @Test
    void geraQrQuePodeSerLidoSemAlterarToken() throws Exception {
        QrCode qr = new QrCode();
        for (int i = 0; i < 100; i++) {
            String token = "token-deterministico-" + i;
            assertEquals(token, qr.ler(qr.gerar(token)));
        }
    }

    @Test
    void rejeitaArquivoQueNaoEhImagem() {
        assertThrows(java.io.IOException.class, () -> new QrCode().ler("texto".getBytes()));
    }
}
