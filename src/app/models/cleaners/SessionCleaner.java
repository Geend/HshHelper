package models.cleaners;

import play.Logger;
import javax.inject.Inject;
import akka.actor.ActorSystem;
import io.ebean.Ebean;
import io.ebean.Update;
import models.UserSession;
import policy.ConstraintValues;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class SessionCleaner {

    private final ActorSystem actorSystem;
    private final ExecutionContext executionContext;

    @Inject
    public SessionCleaner(ActorSystem actorSystem, ExecutionContext executionContext) {
        this.actorSystem = actorSystem;
        this.executionContext = executionContext;

        this.initialize();
    }

    private void initialize() {
        this.actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS), // initialDelay
                Duration.create(1, TimeUnit.MINUTES), // interval
                this::run,
                this.executionContext
        );
    }

    private void run() {
        Logger.info("Session Cleanup starting");

        Update<UserSession> upd = Ebean.createUpdate(UserSession.class,"DELETE FROM USERSESSION  WHERE TIMESTAMPADD(HOUR, :expiry_hours, ISSUED_AT) < CURRENT_TIMESTAMP");
        upd.set("expiry_hours", ConstraintValues.SESSION_TIMEOUT_HOURS);
        int purgedSessions = upd.execute();

        Logger.info("Cleaned "+purgedSessions+" Sessions");
    }
}