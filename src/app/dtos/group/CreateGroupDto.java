package dtos.group;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class CreateGroupDto {
    @Constraints.Required
    @Constraints.MinLength(ConstraintValues.GROUPNAME_MIN_LENGTH)
    @Constraints.MaxLength(ConstraintValues.GROUPNAME_MAX_LENGTH)
    @Constraints.Pattern(ConstraintValues.GROUPNAME_REGEX)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
