package models.finders;

import io.ebean.Finder;
import models.NetServiceCredential;

import java.util.Optional;

public class NetServiceCredentialFinder extends Finder<Long, NetServiceCredential> {

    /**
     * Construct using the default EbeanServer.
     */
    public NetServiceCredentialFinder() {
        super(NetServiceCredential.class);
    }

    public Optional<NetServiceCredential> byIdOptional(Long id) {

        NetServiceCredential credential = this.byId(id);
        if (credential == null) {
            return Optional.empty();
        }

        return Optional.of(credential);
    }
}