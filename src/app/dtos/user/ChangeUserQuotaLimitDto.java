package dtos.user;

import play.data.validation.Constraints;

public class ChangeUserQuotaLimitDto {

    @Constraints.Required
    private Long userId;

    @Constraints.Required
    @Constraints.Min(0)
    private Long newQuotaLimit;


    public Long getNewQuotaLimit() {
        return newQuotaLimit;
    }

    public void setNewQuotaLimit(Long newQuotaLimit) {
        this.newQuotaLimit = newQuotaLimit;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}


