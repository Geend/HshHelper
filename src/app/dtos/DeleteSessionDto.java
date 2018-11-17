package dtos;

import java.util.UUID;

public class DeleteSessionDto {

    public DeleteSessionDto() {
    }

    public DeleteSessionDto(UUID sessionId) {
        this.sessionId = sessionId;
    }

    private UUID sessionId;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

}
