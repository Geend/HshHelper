package managers.netservicemanager;

import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.CryptoResult;
import extension.Crypto.KeyGenerator;
import io.ebean.EbeanServer;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.NetService;
import models.NetServiceCredential;
import models.finders.NetServiceCredentialFinder;
import models.finders.NetServiceFinder;
import play.Logger;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class NetServiceManager {

    private static final Logger.ALogger logger = Logger.of(NetServiceManager.class);

    private final SessionManager sessionManager;
    private final EbeanServer ebeanServer;
    private final NetServiceFinder netServiceFinder;
    private final NetServiceCredentialFinder netServiceCredentialFinder;
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;

    @Inject
    public NetServiceManager(SessionManager sessionManager, EbeanServer ebeanServer, NetServiceFinder netServiceFinder, NetServiceCredentialFinder netServiceCredentialFinder, KeyGenerator keyGenerator, Cipher cipher) {
        this.sessionManager = sessionManager;
        this.ebeanServer = ebeanServer;
        this.netServiceFinder = netServiceFinder;
        this.netServiceCredentialFinder = netServiceCredentialFinder;
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
    }


    public List<NetService> getAllNetServices() throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canSeeAllNetServices())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is looking at all net services.");

        return netServiceFinder.all();
    }

    public void createNetService(String name, String url, String usernameParameterName, String passwordParameterName) throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canCreateNetService())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is creating net service " + name);

        NetService netService = new NetService();
        netService.setName(name);
        netService.setUrl(url);
        netService.setUsernameParameterName(usernameParameterName);
        netService.setPasswordParameterName(passwordParameterName);

        ebeanServer.save(netService);
    }

    public void deleteNetService(Long netServiceId) throws UnauthorizedException, InvalidArgumentException {
        if(!sessionManager.currentPolicy().canDeleteNetServices())
            throw new UnauthorizedException();


        Optional<NetService> netService =  netServiceFinder.byIdOptional(netServiceId);

        if(!netService.isPresent()){
            throw new InvalidArgumentException();
        }

        logger.info(sessionManager.currentUser() + " is deleting net service " + netService.get().getName());

        ebeanServer.delete(netService.get());

    }

    public List<NetServiceCredential> getUserNetServiceCredentials() {
        return sessionManager.currentUser().getNetServiceCredentials();
    }

    public void createNetUserCredential(Long netServiceId, String username, String password) {
        Optional<NetService> netService = netServiceFinder.byIdOptional(netServiceId);
        if(!netService.isPresent()) {
            throw new IllegalArgumentException("Unkown NetServiceId");
        }

        CryptoKey ck = keyGenerator.generate(sessionManager.getCredentialKey());
        CryptoResult encUsername = cipher.encrypt(ck, username.getBytes());
        CryptoResult encPassword = cipher.encrypt(ck, password.getBytes());

        NetServiceCredential credential = new NetServiceCredential();
        credential.setNetService(netService.get());
        credential.setInitializationVectorUsername(encUsername.getInitializationVector());
        credential.setUsernameCipherText(encUsername.getCiphertext());
        credential.setInitializationVectorPassword(encPassword.getInitializationVector());
        credential.setPasswordCipherText(encPassword.getCiphertext());
        credential.setUser(sessionManager.currentUser());
        ebeanServer.save(credential);
    }

    public void deleteNetServiceCredential(Long netServiceCredentialId) {
        //TODO
    }

    public PlaintextCredential decryptCredential(Long netServiceCredentialId) throws UnauthorizedException {
        Optional<NetServiceCredential> optCredential = netServiceCredentialFinder.byIdOptional(netServiceCredentialId);
        if(!optCredential.isPresent()) {
            throw new IllegalArgumentException("credentialId does not exist");
        }

        NetServiceCredential credential = optCredential.get();
        if(!sessionManager.currentPolicy().canReadCredential(credential)) {
            throw new UnauthorizedException();
        }

        CryptoKey ck = keyGenerator.generate(sessionManager.getCredentialKey());

        byte[] usernamePlaintext = cipher.decrypt(ck, credential.getInitializationVectorUsername(), credential.getUsernameCipherText());
        byte[] passwordPlaintext = cipher.decrypt(ck, credential.getInitializationVectorPassword(), credential.getPasswordCipherText());

        return new PlaintextCredential(
            new String(usernamePlaintext),
            new String(passwordPlaintext)
        );
    }

    public NetService getCredentialNetService(Long credentialId) throws UnauthorizedException {
        Optional<NetServiceCredential> optCredential = netServiceCredentialFinder.byIdOptional(credentialId);
        if(!optCredential.isPresent()) {
            throw new IllegalArgumentException("credentialId does not exist");
        }

        NetServiceCredential credential = optCredential.get();
        if(!sessionManager.currentPolicy().canReadCredential(credential)) {
            throw new UnauthorizedException();
        }

        return credential.getNetService();
    }
}
