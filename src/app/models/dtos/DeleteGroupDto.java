package models.dtos;

import play.data.validation.Constraints;
import policy.session.Authentication;

public class DeleteGroupDto {
    @Constraints.Required
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    private Long groupId;
}
