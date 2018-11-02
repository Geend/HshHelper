package extension;

import org.mindrot.jbcrypt.BCrypt;

public class HashHelper {

    public String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }


    public boolean checkHash(String password, String hash){
        return BCrypt.checkpw(password, hash);
    }
}
