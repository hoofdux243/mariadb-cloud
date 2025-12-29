package com.cloud_computing.mariadb.dto.request;

import com.cloud_computing.mariadb.dto.ColumnCreateDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TableCreateRequest {
    @NotBlank(message = "Tên bảng không được để trống")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "Tên bảng chỉ chứa chữ cái, số và dấu gạch dưới")
    String tableName;

    @NotEmpty(message = "Phải có ít nhất 1 cột")
    @Valid
    List<ColumnCreateDTO> columns;
}
