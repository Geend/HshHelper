package extension.crypto;

public class CryptoConstants {

    public static final int AES_KEY_SIZE_BIT = 256;
    public static final int PBKDF2_ROUNDS = 65536;
    public static final int SALT_LENGTH_BYTE = 8;

    public static final String CIPHER_NAME = "AES";
    public static final String CIPHER_NAME_MODE_PADDING = "AES/CBC/PKCS5Padding";
    public static final String KEY_DERIVATION_FUNCTION_NAME = "PBKDF2WithHmacSHA256";

    public static final int GENERATED_KEY_BYTE = 32;
}
