package extension;


import models.User;
import models.UserSession;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UserProvider extends play.mvc.Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        Optional<UserSession> userSession = ContextArguments.getUserSession();
        if(userSession.isPresent()) {
            Long userId = userSession.get().getUserId();
            Optional<User> user = User.find.byIdOptional(userId);
            if(user.isPresent()) {
                ContextArguments.setUser(user.get());
            }
        }
        return delegate.call(ctx);
    }
}