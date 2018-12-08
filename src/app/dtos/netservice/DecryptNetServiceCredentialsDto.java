package dtos.netservice;

import play.data.validation.Constraints;

public class DecryptNetServiceCredentialsDto {
    @Constraints.Required
    private Long credentialId;

    public Long getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }
}
