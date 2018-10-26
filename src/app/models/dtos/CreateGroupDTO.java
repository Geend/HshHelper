package models.dtos;

import io.ebean.Finder;
import models.Group;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import validation.ValidatableWithFinder;
import validation.ValidateWithGroupFinder;

public class CreateGroupDTO {
    @Constraints.Required
    @Constraints.MinLength(3)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
