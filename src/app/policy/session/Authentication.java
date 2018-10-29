package policy.session;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Authentication extends play.mvc.Action.Simple  {
    @With({Authentication.class})
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Required {
    }

    public CompletionStage<Result> call(Http.Context ctx) {
        if(SessionManager.HasActiveSession()) {
            return delegate.call(ctx);
        }

        return CompletableFuture.supplyAsync(() -> redirect(controllers.routes.LoginController.login()));
    }
}
