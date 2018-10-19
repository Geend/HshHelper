import com.google.inject.AbstractModule;
import play.Logger;

public class StartModule extends AbstractModule {
    protected void configure() {
        Logger.info("DatabaseInitialization");
        bind(DatabaseInitialization.class).asEagerSingleton();
    }
}