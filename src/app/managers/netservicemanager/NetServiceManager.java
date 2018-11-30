package managers.netservicemanager;

import io.ebean.EbeanServer;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.NetService;
import models.NetServiceCredential;
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

    @Inject
    public NetServiceManager(SessionManager sessionManager, EbeanServer ebeanServer, NetServiceFinder netServiceFinder) {
        this.sessionManager = sessionManager;
        this.ebeanServer = ebeanServer;
        this.netServiceFinder = netServiceFinder;
    }


    public List<NetService> getAllNetServices() throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canSeeAllNetServices())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is looking at all net services.");

        return netServiceFinder.all();
    }

    public void createNetService(String name) throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canCreateNetService())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is creating net service " + name);

        NetService netService = new NetService();
        netService.setName(name);

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
        //TODO
    }
}
