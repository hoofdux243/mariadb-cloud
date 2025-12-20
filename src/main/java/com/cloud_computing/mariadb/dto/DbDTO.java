package com.cloud_computing.mariadb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbDTO {
    Long id;
    String name;
    Long projectId;
    String projectName;
    Instant createdAt;
    CredentialInfo credentialInfo;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CredentialInfo {
        String hostname;
        Integer port;
        String username;
        String password;
        String connectionString;
    }
}

