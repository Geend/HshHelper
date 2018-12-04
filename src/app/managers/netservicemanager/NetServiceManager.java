package managers.netservicemanager;

import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.CryptoResult;
import extension.Crypto.KeyGenerator;
import extension.logging.DangerousCharFilteringLogger;
import io.ebean.EbeanServer;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.NetService;
import models.NetServiceCredential;
import models.NetServiceParameter;
import models.finders.NetServiceCredentialFinder;
import models.finders.NetServiceFinder;
import play.Logger;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class NetServiceManager {

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(NetServiceManager.class);

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

    public Optional<NetService> getNetService(Long netServiceId) throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canSeeAllNetServices())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is looking at all net " + netServiceId + ".");

        return netServiceFinder.byIdOptional(netServiceId);
    }

    public NetService createNetService(String name, String url) throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canCreateNetService())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is creating net service " + name);

        NetService netService = new NetService();
        netService.setName(name);
        netService.setUrl(url);

        ebeanServer.save(netService);
        return netService;
    }

    public void editNetService(Long netServiceId, String newName, String newUrl) throws UnauthorizedException, InvalidArgumentException {
        Optional<NetService> netServiceOptional = netServiceFinder.byIdOptional(netServiceId);
        if(!netServiceOptional.isPresent()) {
            throw new InvalidArgumentException("Unkown NetServiceId");
        }

        NetService netService = netServiceOptional.get();

        if(!sessionManager.currentPolicy().canEditNetService(netService)){
            throw new UnauthorizedException();
        }

        logger.info(sessionManager.currentUser() + " is editing net service " + netService.getName());

        netService.setName(newName);
        netService.setUrl(newUrl);
        ebeanServer.save(netService);
    }

    public void addNetServiceParameter(Long netServiceId, String name, String defaultValue) throws UnauthorizedException, InvalidArgumentException {

        Optional<NetService> netServiceOpt = getNetService(netServiceId);

        if(!netServiceOpt.isPresent())
            throw new InvalidArgumentException();

        NetService netService = netServiceOpt.get();

        if(!sessionManager.currentPolicy().canEditNetService(netService))
            throw new UnauthorizedException();


        NetServiceParameter parameter = new NetServiceParameter();
        parameter.setName(name);
        if(defaultValue != null){
            parameter.setDefaultValue(defaultValue);
        }

        netService.getParameters().add(parameter);
        ebeanServer.save(netService);

    }


    public void deleteNetService(Long netServiceId) throws UnauthorizedException, InvalidArgumentException {
        if(!sessionManager.currentPolicy().canDeleteNetServices())
            throw new UnauthorizedException();


        Optional<NetService> netServiceOpt =  netServiceFinder.byIdOptional(netServiceId);

        if(!netServiceOpt.isPresent()){
            throw new InvalidArgumentException();
        }

        NetService netService = netServiceOpt.get();

        logger.info(sessionManager.currentUser() + " is deleting net service " + netService.getName());

        ebeanServer.delete(netService);

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
