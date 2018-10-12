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
        Optional<Integer> sessionId = tryParseInt(sessionIdString);
        if(sessionId.isPresent()) {
            Optional<UserSession> userSession = UserSession.findById(sessionId.get());
            if(userSession.isPresent()) {
                ContextArguments.setUserSession(ctx, userSession.get());
            }
        }
        return delegate.call(ctx);
    }

    private Optional<Integer> tryParseInt(String value) {
        if(value == null) {
            return Optional.empty();
        }
        try {
            int i = Integer.parseInt(value);
            return Optional.of(i);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
