import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import managers.loginmanager.LoginManager;
import play.inject.ApplicationLifecycle;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LoginManagerInitialization {
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;
    private final LoginManager loginManager;

    @Inject
    public LoginManagerInitialization(ActorSystem actorSystem, ExecutionContext executionContext, LoginManager loginManager, ApplicationLifecycle lifecycle) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.loginManager = loginManager;

        Cancellable scheduledLoginRecordsCleanup = this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                this.loginManager::deleteOldLoginRecords,
                this.executionContext
        );

        Cancellable scheduledPasswordResetTokensCleanup = this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                this.loginManager::deleteOldPasswordResetTokens,
                this.executionContext
        );

        lifecycle.addStopHook(() -> {
            scheduledLoginRecordsCleanup.cancel();
            scheduledPasswordResetTokensCleanup.cancel();
            // future does not need to do anything. we are canceling the scheduled task, that's enough.
            return CompletableFuture.completedFuture(null);
        });
    }
}
