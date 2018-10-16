import com.google.inject.AbstractModule;
import extension.ApplicationStart;
import play.Logger;

public class StartModule extends AbstractModule {
    protected void configure() {
        Logger.info("ApplicationStart");
        bind(ApplicationStart.class).asEagerSingleton();
    }
}