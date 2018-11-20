package managers.mainmanager;

import models.finders.UserFinder;

import javax.inject.Inject;

public class MainManager {
    private UserFinder userFinder;

    @Inject
    public MainManager(UserFinder userFinder) {
        this.userFinder = userFinder;
    }

    public String getQuotaUsage() {
        return "alles verbraucht";
    }
}
