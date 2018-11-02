import com.google.inject.AbstractModule;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import play.Logger;
import play.mvc.Http;

import javax.inject.Provider;

public class StartModule extends AbstractModule {
    protected void configure() {
        Logger.info("DatabaseInitialization");
        bind(DatabaseInitialization.class).asEagerSingleton();
        bind(LoginFwInitialization.class).asEagerSingleton();
        bind(SessionInitialization.class).asEagerSingleton();
        bind(EbeanServer.class).toProvider(new EbeanServerProvider());
    }

    public class EbeanServerProvider implements Provider<EbeanServer> {
        public EbeanServer get() {
            return Ebean.getDefaultServer();
        }
    }
}