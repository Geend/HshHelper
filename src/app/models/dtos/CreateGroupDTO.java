package models.dtos;

import io.ebean.Finder;
import models.Group;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import validation.ValidatableWithFinder;
import validation.ValidateWithFinder;

@ValidateWithFinder
public class CreateGroupDTO implements ValidatableWithFinder<ValidationError, Group> {
    @Constraints.Required
    @Constraints.MinLength(3)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ValidationError validate(final Finder<Long, Group> finder) {
        if(finder.all().stream().anyMatch(x -> x.name.equalsIgnoreCase(getName()))){
            return new ValidationError("name", "Gruppe existiert bereits!");
        }

        return null;
    }
}
