package adapter.out.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class QrCode {
    public byte[] gerar(String texto) throws IOException {
        try {
            BitMatrix bits = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage imagem = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 300; x++)
                for (int y = 0; y < 300; y++)
                    imagem.setRGB(x, y, bits.get(x, y) ? 0x000000 : 0xffffff);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagem, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Não foi possível gerar QR Code.", e);
        }
    }

}
