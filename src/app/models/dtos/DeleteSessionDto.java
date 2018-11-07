package models.dtos;

import java.util.UUID;

public class DeleteSessionDto {


    private UUID sessionId;

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

}
