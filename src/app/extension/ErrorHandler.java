package extension;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
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