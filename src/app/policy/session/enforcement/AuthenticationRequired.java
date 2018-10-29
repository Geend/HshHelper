package policy.session.enforcement;

import play.mvc.Http;
import play.mvc.Result;
import policy.session.SessionManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthenticationRequired extends play.mvc.Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        if(SessionManager.HasActiveSession()) {
            return delegate.call(ctx);
        }

        return CompletableFuture.supplyAsync(() -> redirect(controllers.routes.LoginController.login()));
    }
}
