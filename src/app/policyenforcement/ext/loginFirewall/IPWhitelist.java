package policyenforcement.ext.loginFirewall;

public interface IPWhitelist {
    public boolean isWhitelisted(String ip);
}
