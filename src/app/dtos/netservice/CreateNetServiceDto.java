package dtos.netservice;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class CreateNetServiceDto {
    @Constraints.Required
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
