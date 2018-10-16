package models;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserSession {

    private Long sessionId;
    private Long userId;
    private DateTime issuedAt;




    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(DateTime issuedAt) {
        this.issuedAt = issuedAt;
    }


    // note: only testcode for the first day, switch later to in memory
    // database h2
    private static List<UserSession> userSessions;

    static {
        userSessions = new ArrayList<UserSession>();
    }
    public static Long sessionsCount() {
        return Long.valueOf(userSessions.size());
    }

    public static List<UserSession> findAll() {
        return new ArrayList<UserSession>(userSessions);
    }


    public static Optional<UserSession> findById(Long id){
        return userSessions.stream().filter(x -> x.getSessionId().equals(id)).findFirst();

    }
    public static void add(UserSession newUserSession) {
        userSessions.add(newUserSession);
    }

    public static void remove(UserSession session) {
       userSessions.remove(session);
    }

}
