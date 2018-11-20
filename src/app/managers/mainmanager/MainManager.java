package managers.mainmanager;

import models.User;
import models.finders.FileFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;

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
        return String.format("%s %% aufgebraucht. (%s, von %s belegt)", percentage, formatSize(totalUsage), formatSize(userQuotaLimit));
    }

    private String formatSize(double sizeInByte) {
        String[] names = new String[8];
        names[0] = "Byte";
        names[1] = "Kilobyte";
        names[2] = "Megabyte";
        names[3] = "Gigabyte";
        names[4] = "Terabyte";
        names[5] = "Petabyte";
        names[6] = "Exabyte";
        names[7] = "Zettabyte";

        int finalIndex = 0;
        for(int i = 0; i < 6; i++) {
            if(sizeInByte > 1000) {
                sizeInByte = sizeInByte / 1000.0;
                finalIndex++;
            }
            else {
                break;
            }
        }
        return String.format("%s %s", sizeInByte, names[finalIndex]);
    }
}
