package com.cloud_computing.mariadb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableDataDTO {
    String name;
    List<String> columns;
    List<Map<String, Object>> rows;
    Long totalRows;
    Long totalColumns;
    Integer page;
    Integer pageSize;
}

