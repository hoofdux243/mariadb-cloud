package com.cloud_computing.mariadb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ColumnModifyDTO {

    @NotBlank(message = "Tên column cũ không được để trống")
    String oldName;

    @NotBlank(message = "Tên column mới không được để trống")
    String newName;

    @NotBlank(message = "Kiểu dữ liệu không được để trống")
    String type;

    String defaultValue;

    String constraints;

    Integer length;
}
