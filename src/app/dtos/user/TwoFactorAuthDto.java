package dtos.user;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class TwoFactorAuthDto {

    //@Constraints.Pattern(ConstraintValues.SECOND_FACTOR_SECRET)
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
