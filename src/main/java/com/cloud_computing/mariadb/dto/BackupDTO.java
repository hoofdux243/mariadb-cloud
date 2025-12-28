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
public class BackupDTO {
    Long id;
    Long userId;
    String userName;
    Long dbId;
    String dbName;
    String fileName;
    Long fileSize;
    String description;
    Instant createdAt;
}
