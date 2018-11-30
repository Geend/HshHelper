package twofactorauth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import play.shaded.ahc.org.asynchttpclient.util.Base64;

public class QrCodeUtil {
    public static String LoadQrCodeImageDataFromGoogle(String identifier, String secret) throws IOException {
        String url = TimeBasedOneTimePasswordUtil.qrImageUrl(identifier, secret);
        URL imageRequest = new URL(url);
        URLConnection connection = imageRequest.openConnection();
        Integer contentLength = connection.getContentLength();
        Integer readBytes = 0;
        InputStream inputStream = connection.getInputStream();
        byte[] qrImageData = new byte[contentLength];
        try {
            while(readBytes != contentLength) {
                Integer rb = inputStream.read(qrImageData, readBytes, contentLength - readBytes);
                if(rb < 0) {
                    break;
                }
                readBytes += rb;
            }
        }
        finally {
            inputStream.close();
        }

        StringBuilder imageDataStringBuilder = new StringBuilder();
        imageDataStringBuilder.append("data:image/png;base64, ");
        imageDataStringBuilder.append(Base64.encode(qrImageData));
        return imageDataStringBuilder.toString();
    }
}
