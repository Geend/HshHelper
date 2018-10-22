package extension;

import constants.CookieConstants;
import models.UserSession;
import models.finders.UserSessionFinder;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UserSessionProvider extends play.mvc.Action.Simple {
    private UserSessionFinder sessionFinder;

    @Inject
    public UserSessionProvider(UserSessionFinder sessionFinder) {
        this.sessionFinder = sessionFinder;
    }

    public CompletionStage<Result> call(Http.Context ctx) {
        String sessionIdString = ctx.session().get(CookieConstants.USER_SESSION_ID_NAME);
        Optional<Long> sessionId = tryParseInt(sessionIdString);
        if(sessionId.isPresent()) {
            Optional<UserSession> userSession = sessionFinder.byIdOptional(sessionId.get());
            if(userSession.isPresent()) {
                ContextArguments.setUserSession(userSession.get());
            }
            else {
                Http.Context.current().session().remove(CookieConstants.USER_SESSION_ID_NAME);
            }
        }
        return delegate.call(ctx);
    }

    private Optional<Long> tryParseInt(String value) {
        if(value == null) {
            return Optional.empty();
        }
        try {
            Long i = Long.parseLong(value);
            return Optional.of(i);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
