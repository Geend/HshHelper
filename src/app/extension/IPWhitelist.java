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
public class IPWhitelist implements policyenforcement.ext.loginFirewall.IPWhitelist {
    private Environment environment;
    private HashSet<String> db;

    @Inject
    public IPWhitelist(Environment environment) throws IOException {
        this.environment = environment;

        db = new HashSet<>();

        InputStream inputStream = environment.resourceAsStream("ip_whitelist.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        while (reader.ready()) {
            String ip = reader.readLine();
            db.add(ip.toLowerCase());
        }
    }


    @Override
    public boolean isWhitelisted(String ip) {
        return db.contains(ip);
    }
}
