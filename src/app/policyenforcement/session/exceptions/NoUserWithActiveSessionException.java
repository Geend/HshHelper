package policyenforcement.session.exceptions;

public class NoUserWithActiveSessionException extends RuntimeException {

    public NoUserWithActiveSessionException(String msg) {
        super(msg);
    }
}
