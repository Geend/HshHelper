package managers.mainmanager;

import models.User;
import models.finders.FileFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;

import static extension.FileSizeFormatter.FormatSize;

public class MainManager {
    private UserFinder userFinder;
    private FileFinder fileFinder;
    private SessionManager sessionManager;

    @Inject
    public MainManager(SessionManager sessionManager, UserFinder userFinder, FileFinder fileFinder) {
        this.sessionManager = sessionManager;
        this.userFinder = userFinder;
        this.fileFinder = fileFinder;
    }

    public String getQuotaUsage() {
        User currentUser = this.sessionManager.currentUser();
        UserQuota userQuota = this.fileFinder.getUsedQuota(currentUser.getUserId());
        double totalUsage = userQuota.getTotalUsage();
        double userQuotaLimit = currentUser.getQuotaLimit();
        int percentage = (int)((100.0 / userQuotaLimit) * totalUsage);
        return String.format("%s %% aufgebraucht. (%s, von %s belegt)", percentage, FormatSize(totalUsage), FormatSize(userQuotaLimit));
    }

    public User currentUser() {
        return sessionManager.currentUser();
    }
}
