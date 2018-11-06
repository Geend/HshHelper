package extension;

import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.ByteBuffer;

public class HashHelper {

    public String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    public boolean checkHash(String password, String hash){
        return BCrypt.checkpw(password, hash);
    }

    public Long insecureStringHash(String input) {
        if(input == null) {
            input = "";
        }

        byte[] data = DigestUtils.md5(input);

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(data, 0, Long.BYTES);
        buffer.flip();

        return buffer.getLong();
    }
}
