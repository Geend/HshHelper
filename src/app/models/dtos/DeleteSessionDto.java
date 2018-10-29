package models.dtos;

import java.util.UUID;

public class DeleteSessionDto {
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    private UUID sessionId;
}
