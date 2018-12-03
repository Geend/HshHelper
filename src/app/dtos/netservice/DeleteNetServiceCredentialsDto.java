package dtos.netservice;

import play.data.validation.Constraints;

public class DeleteNetServiceCredentialsDto {
    @Constraints.Required
    private Long netServiceCredentialId;

    public DeleteNetServiceCredentialsDto() {
    }

    public DeleteNetServiceCredentialsDto(Long netServiceCredentialId) {
        this.netServiceCredentialId = netServiceCredentialId;
    }

    public Long getNetServiceCredentialId() {
        return netServiceCredentialId;
    }

    public void setNetServiceCredentialId(Long netServiceCredentialId) {
        this.netServiceCredentialId = netServiceCredentialId;
    }
}
