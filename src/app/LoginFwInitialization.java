import akka.actor.ActorSystem;
import policy.ext.loginFirewall.Firewall;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class LoginFwInitialization {
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;

    @Inject
    public LoginFwInitialization(ActorSystem actorSystem, ExecutionContext executionContext) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        Firewall.Initialize();

        this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                Firewall::GarbageCollect,
                this.executionContext
        );
    }
}
