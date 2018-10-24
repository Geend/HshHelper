package extension;


import models.User;
import models.UserSession;
import models.finders.UserFinder;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UserProvider extends play.mvc.Action.Simple {
    private UserFinder userFinder;

    @Inject
    public UserProvider(UserFinder userFinder) {
        this.userFinder = userFinder;
    }

    public CompletionStage<Result> call(Http.Context ctx) {
        Optional<UserSession> userSession = ContextArguments.getUserSession();
        if(userSession.isPresent()) {
            Long userId = userSession.get().getUser().getUserId();
            Optional<User> user = userFinder.byIdOptional(userId);
            if(user.isPresent()) {
                ContextArguments.setUser(user.get());
            }
        }
        return delegate.call(ctx);
    }
}