package extension;

import com.typesafe.config.Config;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.Context;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {


    public static String ERROR_KEY = "errormessage";


    @Inject
    public ErrorHandler(Config config, Environment environment,
                        OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(config, environment, sourceMapper, routes);
    }

    protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
        CompletionStage<Result> x = customExceptionHandler(exception);
        if (x != null) return x;

        return super.onProdServerError(request, exception);
    }

    @Override
    protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {
        CompletionStage<Result> x = customExceptionHandler(exception);
        if (x != null) return x;


        return super.onDevServerError(request, exception);
    }

    private CompletionStage<Result> customExceptionHandler(UsefulException exception) {
        if(exception.cause instanceof UnauthorizedException){
            if(exception.cause.getMessage() != null)
                Context.current().flash().put(ERROR_KEY, exception.cause.getMessage());
            return CompletableFuture.completedFuture(
                    Results.redirect(controllers.routes.ErrorController.showForbiddenMessage())
            );
        }
        else if(exception.cause instanceof InvalidArgumentException){
            if(exception.cause.getMessage() != null)
                Context.current().flash().put(ERROR_KEY, exception.cause.getMessage());

            return CompletableFuture.completedFuture(
                    Results.redirect(controllers.routes.ErrorController.showBadRequestMessage())
            );
        }
        return null;
    }


}