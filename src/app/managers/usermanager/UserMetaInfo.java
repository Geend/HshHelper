package managers.usermanager;

public class UserMetaInfo {
    private String username;
    private Integer ownedGroups;
    private Integer ownedFiles;

    protected UserMetaInfo(String username, Integer ownedGroups, Integer ownedFiles) {
        this.username = username;
        this.ownedGroups = ownedGroups;
        this.ownedFiles = ownedFiles;
    }

    public String getUsername() {
        return username;
    }

    protected void setUsername(String username) {
        this.username = username;
    }

    public Integer getOwnedGroups() {
        return ownedGroups;
    }

    protected void setOwnedGroups(Integer ownedGroups) {
        this.ownedGroups = ownedGroups;
    }

    public Integer getOwnedFiles() {
        return ownedFiles;
    }

    protected void setOwnedFiles(Integer ownedFiles) {
        this.ownedFiles = ownedFiles;
    }
}
