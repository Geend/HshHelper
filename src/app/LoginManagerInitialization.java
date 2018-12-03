import akka.actor.ActorSystem;
import managers.loginmanager.LoginManager;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class LoginManagerInitialization {
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;
    private final LoginManager loginManager;

    @Inject
    public LoginManagerInitialization(ActorSystem actorSystem, ExecutionContext executionContext, LoginManager loginManager) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.loginManager = loginManager;

        this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                this.loginManager::deleteOldLoginRecords,
                this.executionContext
        );
    }
}
