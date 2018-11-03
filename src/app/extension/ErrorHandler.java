package extension;

import controllers.routes;
import domainlogic.UnauthorizedException;
import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.*;
import play.mvc.*;
import com.typesafe.config.Config;

import javax.inject.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

    @Inject
    public ErrorHandler(Config config, Environment environment,
                        OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(config, environment, sourceMapper, routes);
    }

    protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
        if(exception.cause instanceof UnauthorizedException){
            return CompletableFuture.completedFuture(
                    Results.redirect(controllers.routes.ForbiddenController.showForbiddenMessage())
            );
        }

        return super.onProdServerError(request, exception);
    }

    @Override
    protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {

        if(exception.cause instanceof UnauthorizedException){
            return CompletableFuture.completedFuture(
                    Results.redirect(controllers.routes.ForbiddenController.showForbiddenMessage())
            );
        }
        return super.onDevServerError(request, exception);
    }

}