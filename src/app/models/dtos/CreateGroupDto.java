package models.dtos;

import play.data.validation.Constraints;

public class CreateGroupDto {
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
