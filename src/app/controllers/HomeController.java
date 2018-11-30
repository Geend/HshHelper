package controllers;

import extension.CredentialSerializer.Credential;
import extension.CredentialSerializer.CredentialFactory;
import extension.CredentialSerializer.Serializer;
import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.KeyGenerator;
import extension.RandomDataGenerator;
import managers.filemanager.FileManager;
import managers.filemanager.dto.FileMeta;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.security.SecureRandom;
import java.util.List;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class HomeController extends Controller {
    private final SessionManager sessionManager;
    private final FileManager fileManager;
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;

    @Inject
    public HomeController(SessionManager sessionManager, FileManager fileManager, KeyGenerator keyGenerator, Cipher cipher) {
        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
    }

    public Result index() {
        User u = sessionManager.currentUser();

        CryptoKey key = keyGenerator.generate("admin", u.getCryptoSalt());
        byte[] plain = cipher.decrypt(key, u.getInitializationVectorCredentialKey(), u.getCredentialKeyCipherText());

        CredentialFactory cf = new CredentialFactory(new RandomDataGenerator(new SecureRandom()));
        Credential c = cf.create("asd", "xxx");

        Serializer serializer = new Serializer();
        serializer.serialize(c);

        List<FileMeta> accessibleFiles = fileManager.accessibleFiles();
        return ok(views.html.Index.render(asScala(accessibleFiles)));
    }

}