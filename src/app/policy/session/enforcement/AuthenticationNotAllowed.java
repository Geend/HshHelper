package policy.session.enforcement;

import controllers.routes;
import play.mvc.Http;
import play.mvc.Result;
import policy.session.SessionManager;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthenticationNotAllowed extends play.mvc.Action.Simple {
    private final SessionManager sessionManager;

    @Inject
    public AuthenticationNotAllowed(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public CompletionStage<Result> call(Http.Context ctx) {
        if(!sessionManager.hasActiveSession()) {
            return delegate.call(ctx);
        }

        return CompletableFuture.supplyAsync(() -> redirect(controllers.routes.HomeController.index()));
    }
}
