import io.ebean.Ebean;
import io.ebean.EbeanServer;
import play.api.Configuration;
import play.api.Environment;
import play.api.db.DBApi;
import play.api.db.evolutions.DynamicEvolutions;
import play.api.inject.ApplicationLifecycle;
import play.db.ebean.EbeanConfig;

import javax.inject.Inject;
import javax.inject.Provider;

public class EBeanServerProvider implements Provider<EbeanServer> {

    @Inject
    public DynamicEvolutions dynamicEvolution;
    @Inject
    public ApplicationLifecycle lifecycle;
    @Inject
    public Environment environment;
    @Inject
    public Configuration configuration;
    @Inject
    public EbeanConfig ebeanConfig;
    @Inject
    public DBApi dbApi;

    public EbeanServer get() {
        return Ebean.getDefaultServer();
    }
}