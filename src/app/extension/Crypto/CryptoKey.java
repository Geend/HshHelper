package extension.Crypto;

import javax.crypto.SecretKey;

public class CryptoKey {
    private SecretKey key;


    CryptoKey(SecretKey key) {
        this.key = key;
    }

    public SecretKey getKey() {
        return key;
    }
}
