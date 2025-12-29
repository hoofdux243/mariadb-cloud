package com.cloud_computing.mariadb.dto.request;

import com.cloud_computing.mariadb.dto.ColumnCreateDTO;
import com.cloud_computing.mariadb.dto.ColumnModifyDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TableAlterRequest {

    List<@Valid ColumnCreateDTO> addColumns;

    List<String> dropColumns;

    List<ColumnModifyDTO> modifyColumns;
}
