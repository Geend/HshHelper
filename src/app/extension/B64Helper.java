package extension;

import java.util.Base64;

public class B64Helper {
    public String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public byte[] decode(String data) {
        return Base64.getDecoder().decode(data.getBytes());
    }
}
