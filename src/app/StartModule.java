import com.google.inject.AbstractModule;
import models.finders.UserFinder;
import play.Logger;

public class StartModule extends AbstractModule {
    protected void configure() {
        Logger.info("DatabaseInitialization");
        bind(DatabaseInitialization.class).asEagerSingleton();
        bind(LoginFwInitialization.class).asEagerSingleton();
        bind(SessionInitialization.class).asEagerSingleton();
    }
}