import akka.actor.ActorSystem;
import policy.ext.loginFirewall.Firewall;
import policy.session.SessionManager;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class SessionInitialization {
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;

    @Inject
    public SessionInitialization(ActorSystem actorSystem, ExecutionContext executionContext) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;

        this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                SessionManager::GarbageCollect,
                this.executionContext
        );
    }
}
