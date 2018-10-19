package models.dtos;

public class DeleteSessionDTO {
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    private Long sessionId;
}
