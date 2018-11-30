package extension.Crypto;

public class CryptoResult {
    private byte[] initializationVector;
    private byte[] ciphertext;


    CryptoResult(byte[] initializationVector, byte[] ciphertext) {
        this.initializationVector = initializationVector;
        this.ciphertext = ciphertext;
    }

    public byte[] getInitializationVector() {
        return initializationVector;
    }

    public byte[] getCiphertext() {
        return ciphertext;
    }
}
