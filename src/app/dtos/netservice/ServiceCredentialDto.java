package dtos.netservice;

import java.util.List;

public class ServiceCredentialDto {
    public static class ServiceParameter {
        private String name;
        private String value;

        public ServiceParameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    private String username;
    private String password;
    private String authUrl;
    private List<ServiceParameter> parameterList;

    public ServiceCredentialDto(String username, String password, String authUrl, List<ServiceParameter> parameterList) {
        this.username = username;
        this.password = password;
        this.authUrl = authUrl;
        this.parameterList = parameterList;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public List<ServiceParameter> getParameterList() {
        return parameterList;
    }
}
