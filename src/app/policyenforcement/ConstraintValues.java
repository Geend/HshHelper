package policyenforcement;

public class ConstraintValues {

    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MAX_PASSWORD_LENGTH = 72;
    public static final String USERNAME_REGEX = "[a-z][a-z0-9.]+";


    public static final int GROUPNAME_MIN_LENGTH = 3;
    public static final int GROUPNAME_MAX_LENGTH = 20;
    public static final String GROUPNAME_REGEX = "[a-z][a-z0-9.]+";


    public static final int MIN_SESSION_TIMEOUT_MINUTES = 5;

    public static final int MAX_SESSION_TIMEOUT_HOURS = 24;

}
