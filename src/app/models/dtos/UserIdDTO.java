package models.dtos;

// AddUserToGroupDTO, UserListEntryDTO, RemoveGroupUserDTO, DeleteUserDTO
public class UserIdDTO {
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}