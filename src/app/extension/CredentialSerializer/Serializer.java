package extension.CredentialSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Serializer {
    public byte[] serialize(Credential credential) {
        Gson gson = new GsonBuilder().create();
        String jsonOutput = gson.toJson(credential);
        return jsonOutput.getBytes();
    }
}
