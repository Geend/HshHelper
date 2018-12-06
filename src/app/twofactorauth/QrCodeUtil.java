package twofactorauth;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
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
        byte[] qrImageData = null;
        try {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
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
