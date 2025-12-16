package com.cloud_computing.mariadb.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class APIResponse {
    int code;
    String message;
    Object data;
}
