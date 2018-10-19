package models.dtos;

import models.Group;
import models.finders.GroupFinder;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import javax.inject.Inject;

@Constraints.Validate
public class CreateGroupDTO implements Constraints.Validatable<ValidationError> {
    @Constraints.Required
    @Constraints.MinLength(3)
    private String name;

    private GroupFinder groupFinder;

    @Inject
    public CreateGroupDTO(GroupFinder groupFinder) {
        this.groupFinder = groupFinder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ValidationError validate() {
        if(groupFinder.all().stream().anyMatch(x -> x.name.equalsIgnoreCase(getName()))){
            return new ValidationError("name", "Gruppe existiert bereits!");
        }

        return null;
    }
}
