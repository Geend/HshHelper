package policyenforcement;

public class ConstraintValues {

    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MAX_PASSWORD_LENGTH = 72;
    public static final String USERNAME_REGEX = "[a-z][a-z0-9.]+";
    public static final String SECOND_FACTOR_NUMBER = "[0-9 ]+";

    public static final String SECOND_FACTOR_SECRET = "[A-Z2-7]{16}";

    public static final int MAX_FILENAME_LENGTH = 40;
    public static final String FILENAME_REGEX = "[a-zA-Zäüöß0-9.,#%+&!\"':;~-]+";


    public static final int GROUPNAME_MIN_LENGTH = 3;
    public static final int GROUPNAME_MAX_LENGTH = 20;
    public static final String GROUPNAME_REGEX = "[a-zA-Z][a-zA-Z0-9.]+";

    public static final String RETURN_URL_REGEX = "\\/[a-zA-Z0-9][a-zA-Z0-9\\/]+";


    public static final int MIN_SESSION_TIMEOUT_MINUTES = 5;

    public static final int MAX_SESSION_TIMEOUT_HOURS = 24;

    public static final int SUCCESSFUL_LOGIN_STORAGE_DURATION_DAYS = 60;

    public static final int PASSWORD_RESET_TOKEN_TIMEOUT_HOURS = 24;

    public static final int GENREATED_PASSWORD_LENGTH = 12;

}
