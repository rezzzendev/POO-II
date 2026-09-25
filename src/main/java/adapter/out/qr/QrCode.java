package adapter.out.qr;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.*;

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

    public String ler(byte[] png) throws IOException {
        BufferedImage im = ImageIO.read(new ByteArrayInputStream(png));
        if (im == null || im.getWidth() > 4096 || im.getHeight() > 4096)
            throw new IllegalArgumentException("Imagem QR inválida ou muito grande.");
        int[] pixels = im.getRGB(0, 0, im.getWidth(), im.getHeight(), null, 0, im.getWidth());
        var bitmap =
                new BinaryBitmap(
                        new com.google.zxing.common.HybridBinarizer(
                                new RGBLuminanceSource(im.getWidth(), im.getHeight(), pixels)));
        var dicas = new java.util.EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        dicas.put(DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(BarcodeFormat.QR_CODE));
        dicas.put(DecodeHintType.TRY_HARDER, true);
        try {
            return new MultiFormatReader().decode(bitmap, dicas).getText();
        } catch (NotFoundException e) {
            // PNG digital pode dispensar a detecção geométrica usada em fotografias.
            dicas.put(DecodeHintType.PURE_BARCODE, true);
            try {
                return new MultiFormatReader().decode(bitmap, dicas).getText();
            } catch (NotFoundException invalido) {
                throw new IllegalArgumentException("QR Code não encontrado na imagem.");
            }
        }
    }
}
