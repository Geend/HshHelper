package dtos.group;

import play.data.validation.Constraints;

public class UserIdDto {

    @Constraints.Required
    private Long userId;

    public UserIdDto() {

    }

    public UserIdDto(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
