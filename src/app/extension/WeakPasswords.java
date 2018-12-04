package extension;

import play.Environment;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

@Singleton
public class WeakPasswords {
    private Environment environment;
    private HashSet<String> db;

    @Inject
    public WeakPasswords(Environment environment) throws IOException {
        this.environment = environment;

        db = new HashSet<>();

        InputStream inputStream = environment.resourceAsStream("password_blacklist.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        while (reader.ready()) {
            String weapPw = reader.readLine();
            db.add(weapPw.toLowerCase());
        }
    }

    public boolean isWeakPw(String pw) {
        return db.contains(pw.toLowerCase());
    }
}
