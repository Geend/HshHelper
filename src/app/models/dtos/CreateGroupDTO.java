package models.dtos;

import play.data.validation.Constraints;
import policy.ConstraintValues;

public class CreateGroupDTO {
    @Constraints.Required
    @Constraints.MinLength(ConstraintValues.GROUPNAME_MAX_LENGTH)
    @Constraints.Pattern(ConstraintValues.GROUPNAME_REGEX)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
