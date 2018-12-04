import com.google.inject.AbstractModule;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import play.Logger;
import views.TemplateEnvironment;

import javax.inject.Provider;

public class StartModule extends AbstractModule {
    protected void configure() {
        Logger.info("DatabaseInitialization");
        bind(DatabaseInitialization.class).asEagerSingleton();
        bind(LoginFwInitialization.class).asEagerSingleton();
        bind(SessionInitialization.class).asEagerSingleton();
        bind(LoginManagerInitialization.class).asEagerSingleton();
        bind(EbeanServer.class).toProvider(new EBeanServerProvider());

        requestStaticInjection(TemplateEnvironment.class);
    }
}