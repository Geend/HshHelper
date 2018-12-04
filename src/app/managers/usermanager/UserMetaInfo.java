package managers.usermanager;

public class UserMetaInfo {
    private String username;
    private Integer ownedGroups;
    private Boolean has2FA;

    protected UserMetaInfo(String username, Integer ownedGroups, Boolean has2FA) {
        this.username = username;
        this.ownedGroups = ownedGroups;
        this.has2FA = has2FA;
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

    public Boolean getHas2FA() {
        return has2FA;
    }

    public void setHas2FA(Boolean has2FA) {
        this.has2FA = has2FA;
    }
}
