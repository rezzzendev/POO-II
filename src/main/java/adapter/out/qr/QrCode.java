package adapter.out.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

public class QrCode {
    private static final int TAMANHO = 420;

    public byte[] gerar(String texto) throws IOException {
        try {
            Map<EncodeHintType, Object> opcoes = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 4);
            BitMatrix bits = new QRCodeWriter().encode(
                    texto, BarcodeFormat.QR_CODE, TAMANHO, TAMANHO, opcoes);
            BufferedImage imagem = new BufferedImage(TAMANHO, TAMANHO, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < TAMANHO; x++)
                for (int y = 0; y < TAMANHO; y++)
                    imagem.setRGB(x, y, bits.get(x, y) ? 0x000000 : 0xffffff);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(imagem, "PNG", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Não foi possível gerar QR Code.", e);
        }
    }

    /** Lê novamente uma imagem QR; usado para validar o PNG gerado e em testes de integração. */
    public String ler(byte[] imagem) throws IOException {
        BufferedImage entrada = ImageIO.read(new ByteArrayInputStream(imagem));
        if (entrada == null) throw new IOException("O arquivo não é uma imagem válida.");

        int largura = entrada.getWidth();
        int altura = entrada.getHeight();
        int[] pixels = entrada.getRGB(0, 0, largura, altura, null, 0, largura);
        BinaryBitmap codigo = new BinaryBitmap(
                new HybridBinarizer(new RGBLuminanceSource(largura, altura, pixels)));
        try {
            return new MultiFormatReader().decode(
                    codigo,
                    Map.of(
                            DecodeHintType.PURE_BARCODE, Boolean.TRUE,
                            DecodeHintType.TRY_HARDER, Boolean.TRUE))
                    .getText();
        } catch (NotFoundException e) {
            throw new IOException("A imagem não contém um QR Code legível.", e);
        }
    }

}
