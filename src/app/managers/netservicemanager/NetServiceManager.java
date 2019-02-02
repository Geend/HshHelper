package managers.netservicemanager;

import extension.crypto.Cipher;
import extension.crypto.CryptoKey;
import extension.crypto.CryptoResult;
import extension.crypto.KeyGenerator;
import extension.logging.DangerousCharFilteringLogger;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
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
        if (!sessionManager.currentPolicy().canSeeAllNetServices())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is looking at all net services.");

        return netServiceFinder.all();
    }

    public NetService getNetService(Long netServiceId) throws UnauthorizedException, InvalidArgumentException {
        if (!sessionManager.currentPolicy().canSeeAllNetServices())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is looking at net service " + netServiceId + ".");

        Optional<NetService> netService = netServiceFinder.byIdOptional(netServiceId);
        if(!netService.isPresent()) {
            throw new InvalidArgumentException("NetService existiert nicht");
        }

        return netService.get();
    }

    public NetService createNetService(String name, String url) throws UnauthorizedException, NetServiceAlreadyExistsException {
        if (!sessionManager.currentPolicy().canCreateNetService())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is creating net service " + name);

        NetService netService;
        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<NetService> optNetService = netServiceFinder.byName(name);
            if(optNetService.isPresent()) {
                throw new NetServiceAlreadyExistsException();
            }

            netService = new NetService();
            netService.setName(name);
            netService.setUrl(url);

            ebeanServer.save(netService);

            tx.commit();
        }

        return netService;
    }

    public void editNetService(Long netServiceId, String newName, String newUrl) throws UnauthorizedException, InvalidArgumentException {
        Optional<NetService> netServiceOptional = netServiceFinder.byIdOptional(netServiceId);
        if (!netServiceOptional.isPresent()) {
            throw new InvalidArgumentException("Unkown NetServiceId");
        }

        NetService netService = netServiceOptional.get();

        if (!sessionManager.currentPolicy().canEditNetService()) {
            throw new UnauthorizedException();
        }

        logger.info(sessionManager.currentUser() + " is editing net service " + netService.getName());

        netService.setName(newName);
        netService.setUrl(newUrl);
        ebeanServer.save(netService);
    }

    public void addNetServiceParameter(Long netServiceId, NetServiceParameter.NetServiceParameterType type, String name, String defaultValue) throws UnauthorizedException, InvalidArgumentException {
        NetService netService = getNetService(netServiceId);

        if (!sessionManager.currentPolicy().canEditNetService())
            throw new UnauthorizedException();

        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            if (type == NetServiceParameter.NetServiceParameterType.USERNAME || type == NetServiceParameter.NetServiceParameterType.PASSWORD) {
                boolean typeAlreadyExists = netService.getParameters().stream().anyMatch(x -> x.getParameterType() == type);
                if (typeAlreadyExists) {
                    throw new InvalidArgumentException("Ein Parameter mit diesem Type existiert bereits.");
                }
            }

            boolean nameAlreadyExists = netService.getParameters().stream().anyMatch(x -> x.getName().equals(name));
            if (nameAlreadyExists) {
                throw new InvalidArgumentException("Ein Parameter mit diesem Namen existiert bereits.");
            }

            NetServiceParameter parameter = new NetServiceParameter();
            parameter.setName(name);
            if (defaultValue != null) {
                parameter.setDefaultValue(defaultValue);
            }
            parameter.setParameterType(type);

            netService.getParameters().add(parameter);
            ebeanServer.save(netService);

            tx.commit();

        }

    }

    public void removeNetServiceParameter(Long netServiceId, Long netServiceParameterId) throws UnauthorizedException, InvalidArgumentException {
        NetService netService = getNetService(netServiceId);

        if (!sessionManager.currentPolicy().canEditNetService())
            throw new UnauthorizedException();

        Optional<NetServiceParameter> parameter = netService.getParameters().stream().filter(x -> x.getNetServiceParameterId().equals(netServiceParameterId)).findAny();

        if (parameter.isPresent()) {
            netService.getParameters().remove(parameter.get());
            ebeanServer.save(netService);
            ebeanServer.delete(parameter.get());
        } else {
            throw new InvalidArgumentException();
        }
    }

    public void deleteNetService(Long netServiceId) throws UnauthorizedException, InvalidArgumentException {
        if (!sessionManager.currentPolicy().canDeleteNetServices())
            throw new UnauthorizedException();


        Optional<NetService> netServiceOpt = netServiceFinder.byIdOptional(netServiceId);

        if (!netServiceOpt.isPresent()) {
            throw new InvalidArgumentException();
        }

        NetService netService = netServiceOpt.get();

        logger.info(sessionManager.currentUser() + " is deleting net service " + netService.getName());

        ebeanServer.delete(netService);

    }

    public List<NetServiceCredential> getUserNetServiceCredentials() {
        return sessionManager.currentUser().getNetServiceCredentials();
    }

    public void createNetUserCredential(Long netServiceId, String username, String password) throws InvalidArgumentException {
        Optional<NetService> netService = netServiceFinder.byIdOptional(netServiceId);
        if (!netService.isPresent()) {
            throw new InvalidArgumentException("Unkown NetServiceId");
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

    public void deleteNetServiceCredential(Long netServiceCredentialId) throws InvalidArgumentException, UnauthorizedException {
        Optional<NetServiceCredential> credentialOpt = netServiceCredentialFinder.byIdOptional(netServiceCredentialId);

        if(!credentialOpt.isPresent()){
            throw new InvalidArgumentException();
        }

        if(!sessionManager.currentPolicy().canDeleteNetServicesCredential(credentialOpt.get())){
            throw new UnauthorizedException();
        }

        ebeanServer.delete(credentialOpt.get());
    }

    public NetServiceCredential getEncryptedCredential(Long netServiceCredentialId) throws UnauthorizedException, InvalidArgumentException {
        Optional<NetServiceCredential> optCredential = netServiceCredentialFinder.byIdOptional(netServiceCredentialId);
        if (!optCredential.isPresent()) {
            throw new InvalidArgumentException("credentialId does not exist");
        }

        NetServiceCredential credential = optCredential.get();
        if (!sessionManager.currentPolicy().canReadCredential(credential)) {
            throw new UnauthorizedException();
        }

        return optCredential.get();
    }

    public PlaintextCredential decryptCredential(NetServiceCredential credential) {
        CryptoKey ck = keyGenerator.generate(sessionManager.getCredentialKey());

        byte[] usernamePlaintext = cipher.decrypt(ck, credential.getInitializationVectorUsername(), credential.getUsernameCipherText());
        byte[] passwordPlaintext = cipher.decrypt(ck, credential.getInitializationVectorPassword(), credential.getPasswordCipherText());

        return new PlaintextCredential(
                new String(usernamePlaintext),
                new String(passwordPlaintext)
        );
    }

}
