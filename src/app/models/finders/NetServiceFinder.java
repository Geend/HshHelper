package models.finders;

import io.ebean.Finder;
import models.Group;
import models.NetService;
import sun.nio.ch.Net;

import java.util.Optional;

public class NetServiceFinder extends Finder<Long, NetService> {

    /**
     * Construct using the default EbeanServer.
     */
    public NetServiceFinder() {
        super(NetService.class);
    }

    public Optional<NetService> byIdOptional(Long id) {

        NetService netService = this.byId(id);
        if (netService == null) {
            return Optional.empty();
        }
        return Optional.of(netService);
    }


    public Optional<NetService> byName(String name) {
        return this.query().where().eq("name", name).findOneOrEmpty();
    }


}