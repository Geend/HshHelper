package models.dtos;

import play.data.validation.Constraints;

public class UserIdDto {

    @Constraints.Required
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
