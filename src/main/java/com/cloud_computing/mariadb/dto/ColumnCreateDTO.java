package com.cloud_computing.mariadb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnCreateDTO {
    @NotBlank(message = "Tên column không được để trống")
    String name;
    @NotBlank(message = "Kiểu dữ liệu không được để trống")
    String type;
    String defaultValue;
    String constraints;
    Integer length; //length varchar

    String foreignKeyTable;
    String foreignKeyColumn;
    String onDelete;
    String onUpdate;
}
