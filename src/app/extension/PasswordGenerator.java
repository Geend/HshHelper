package extension;

import java.security.SecureRandom;

public class PasswordGenerator {


    //TODO: Include validChars for generated Passwords in policy
    private static final String validChars = "!%?#-_*+0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static SecureRandom secureRandom = new SecureRandom();

    public String generatePassword(int length) {

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++){
            int index = secureRandom.nextInt(validChars.length());
            sb.append(validChars.charAt(index));
        }
        return sb.toString();
    }


}
