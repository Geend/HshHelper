package extension;

import org.mindrot.jbcrypt.BCrypt;

public class HashHelper {

    public static String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt(16));
    }


    public static boolean checkHash(String password, String hash){
        return BCrypt.checkpw(password, hash);
    }
}
