import com.google.inject.AbstractModule;
import models.cleaners.SessionCleaner;
import models.finders.UserFinder;
import models.finders.UserSessionFinder;
import play.Logger;

public class StartModule extends AbstractModule {
    protected void configure() {
        Logger.info("DatabaseInitialization");
        bind(DatabaseInitialization.class).asEagerSingleton();
        bind(SessionCleaner.class).asEagerSingleton();
        bind(UserSessionFinder.class);
        bind(UserFinder.class);


    }
}