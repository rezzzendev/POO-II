package adapter.out.qr;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodeTest {
    @Test
    void geraImagemPngDeTamanhoEsperado() throws Exception {
        byte[] png = new QrCode().gerar("token-de-teste");
        var imagem = ImageIO.read(new ByteArrayInputStream(png));

        assertNotNull(imagem);
        assertEquals(300, imagem.getWidth());
        assertEquals(300, imagem.getHeight());
    }
}
