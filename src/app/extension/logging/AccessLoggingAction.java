package extension.logging;

import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

public class AccessLoggingAction implements play.http.ActionCreator {

    private Logger.ALogger accessLogger;
    private SessionManager sessionManager;

    @Inject
    public AccessLoggingAction(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        accessLogger = new DangerousCharFilteringLogger("access");
    }

    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        return new Action.Simple() {
            @Override
            public CompletionStage<Result> call(Http.Context ctx) {
                // This is a "workaround" so that the class is usable in contexts where we
                // cannot extract a user from the current HTTP context, e.g. on an endpoint
                // where no authorized user is required like /login
                String username = "unauthorized user";
                try {
                    username = sessionManager.currentUser().getUsername();
                } catch (RuntimeException e) {
                    if (!e.getMessage().equals("There is no active session")) {
                        throw e;
                    }
                }
                accessLogger.info(request.remoteAddress() + " - "
                                + username + " \"" +  request.method() + " " + request.uri() + "\"");

                return delegate.call(ctx);
            }
        };
    }
}