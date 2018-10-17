package models.dtos;

import models.Group;
import models.finders.GroupFinder;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

@Constraints.Validate
public class CreateGroupDTO implements Constraints.Validatable<ValidationError> {
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
    public ValidationError validate() {
        if(Group.find.all().stream().anyMatch(x -> x.name.equalsIgnoreCase(getName()))){
            return new ValidationError("name", "Gruppe existiert bereits!");
        }

        return null;
    }
}
