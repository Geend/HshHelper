import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import play.inject.ApplicationLifecycle;
import policyenforcement.session.SessionManager;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SessionInitialization {
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;
    private final SessionManager sessionManager;

    @Inject
    public SessionInitialization(ActorSystem actorSystem, ExecutionContext executionContext, SessionManager sessionManager, ApplicationLifecycle lifecycle) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.sessionManager = sessionManager;

        Cancellable scheduledTask = this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                this.sessionManager::garbageCollect,
                this.executionContext
        );

        lifecycle.addStopHook(() -> {
            scheduledTask.cancel();
            // future does not need to do anything. we are canceling the scheduled task, that's enough.
            return CompletableFuture.completedFuture(null);
        });
    }
}
