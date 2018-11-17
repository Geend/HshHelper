package models.dtos;

import play.data.validation.Constraints;

public class DeleteGroupDto {
    @Constraints.Required
    private Long groupId;

    public DeleteGroupDto() {
    }

    public DeleteGroupDto(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
