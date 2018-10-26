package extension;

import models.UserSession;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthenticationRequiredAnnotation extends play.mvc.Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        String remoteAddress = ctx.request().remoteAddress();
        Optional<UserSession> userSession = ContextArguments.getUserSession();
        if(userSession.isPresent() && remoteAddress.equals(userSession.get().getConnectedFrom())) {
            return delegate.call(ctx);
        }

        return CompletableFuture.supplyAsync(() -> redirect(controllers.routes.LoginController.login()));
    }
}
