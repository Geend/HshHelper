package extension;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.Base64;

public class RandomDataGenerator {
    private SecureRandom random;

    @Inject
    public RandomDataGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generateString(int minBytes, int maxBytes) {
        int genBytes = random.nextInt(maxBytes) + minBytes;
        byte[] randomData = new byte[genBytes];
        random.nextBytes(randomData);
        byte[] b64RandomData = Base64.getEncoder().encode(randomData);
        return new String(b64RandomData);
    }

    public byte[] generateBytes(int num) {
        byte[] randomData = new byte[num];
        random.nextBytes(randomData);
        return randomData;
    }
}
