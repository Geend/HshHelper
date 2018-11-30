package extension.CredentialSerializer;

import extension.RandomDataGenerator;

import javax.inject.Inject;

public class CredentialFactory {
    private RandomDataGenerator randomDataGenerator;

    @Inject
    public CredentialFactory(RandomDataGenerator randomDataGenerator) {
        this.randomDataGenerator = randomDataGenerator;
    }

    public Credential create(String username, String password) {
        String noise = randomDataGenerator.generateString(100, 200);
        return new Credential(username, password, noise);
    }
}
