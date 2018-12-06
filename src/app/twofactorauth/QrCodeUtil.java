package twofactorauth;

import ar.com.hjg.pngj.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import play.shaded.ahc.org.asynchttpclient.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QrCodeUtil {
    public static String LoadQrCodeImageDataFromGoogle(String identifier, String secret) throws IOException, WriterException {

        String qrCodeData = String.format("otpauth://totp/%s?secret=%s", identifier, secret);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] qrImageData;
        try {
            ImageInfo imageInfo = new ImageInfo(200, 200, 1, false, true, false);
            PngWriter pngWriter = new PngWriter(byteArrayOutputStream, imageInfo);
            for (int y = 0; y < 200; y++) {
                ImageLineByte imageLine = new ImageLineByte(imageInfo);
                for (int x = 0; x < 200; x++) {
                    boolean pixel = bitMatrix.get(x, y);
                    byte pixelValue = pixel ? (byte)1 : 0;
                    imageLine.getScanline()[x] = pixelValue;
                }
                pngWriter.writeRow(imageLine, y);
            }
            pngWriter.end();
            qrImageData = byteArrayOutputStream.toByteArray();
        }
        finally {
            byteArrayOutputStream.close();
        }

        StringBuilder imageDataStringBuilder = new StringBuilder();
        imageDataStringBuilder.append("data:image/png;base64, ");
        imageDataStringBuilder.append(Base64.encode(qrImageData));
        return imageDataStringBuilder.toString();
    }
}
