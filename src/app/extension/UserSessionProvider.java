package extension;

import constants.CookieConstants;
import models.UserSession;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UserSessionProvider extends play.mvc.Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        String sessionIdString = ctx.session().get(CookieConstants.USER_SESSION_ID_NAME);
        Optional<Long> sessionId = tryParseInt(sessionIdString);
        if(sessionId.isPresent()) {
            Optional<UserSession> userSession = UserSession.findById(sessionId.get());
            if(userSession.isPresent()) {
                ContextArguments.setUserSession(ctx, userSession.get());
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
