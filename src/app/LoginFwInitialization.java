import akka.actor.ActorSystem;
import policyenforcement.ext.loginFirewall.Firewall;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class LoginFwInitialization {
    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;
    private final Firewall firewall;

    @Inject
    public LoginFwInitialization(ActorSystem actorSystem, ExecutionContext executionContext, Firewall firewall) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;
        this.firewall = firewall;
        this.firewall.initialize();

        this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(5, TimeUnit.MINUTES), // interval
                this.firewall::garbageCollect,
                this.executionContext
        );
    }
}
