package extension;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordGeneratorTest {

    @Test
    public void generatePassword() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        String password = passwordGenerator.generatePassword(10);

        Assert.assertEquals(10, password.length());
    }

    @Test
    public void generateMultiplePasswords() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        String password1 = passwordGenerator.generatePassword(12);
        String password2 = passwordGenerator.generatePassword(12);

        //TODO: Is it possible that the same password is generated twice in a row?
        Assert.assertNotSame(password1, password2);
    }
}