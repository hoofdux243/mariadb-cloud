package com.cloud_computing.mariadb.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Builder
public class ExceptionResponse {
    int code;
    String message;
}
