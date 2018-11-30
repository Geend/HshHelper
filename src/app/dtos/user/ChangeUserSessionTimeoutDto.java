package dtos.user;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class ChangeUserSessionTimeoutDto {

    @Constraints.Min(ConstraintValues.MIN_SESSION_TIMEOUT_MINUTES)
    @Constraints.Max(60 * ConstraintValues.MAX_SESSION_TIMEOUT_HOURS)
    @Constraints.Required
    private Integer valueInMinutes;

    public Integer getValueInMinutes() {
        return valueInMinutes;
    }

    public void setValueInMinutes(Integer valueInMinutes) {
        this.valueInMinutes = valueInMinutes;
    }
}
