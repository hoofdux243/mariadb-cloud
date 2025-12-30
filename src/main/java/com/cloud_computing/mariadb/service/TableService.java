package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.RowDTO;
import com.cloud_computing.mariadb.dto.TableDataDTO;
import com.cloud_computing.mariadb.dto.request.TableAlterRequest;
import com.cloud_computing.mariadb.dto.request.TableCreateRequest;

import java.util.List;
import java.util.Map;

public interface TableService {
    void createTable(Long dbId, TableCreateRequest dto);
    void alterColumn(Long dbId, String tableName, TableAlterRequest request);
    void renameTable(Long dbId, String oldName, String newName);
    void dropTable(Long dbId, String tableName);
    List<TableDataDTO> getTables(Long dbId);
    Map<String, Object> getTableStructure(Long dbId, String tableName);
    TableDataDTO getTableData(Long dbId, String tableName, int page, int pageSize);
    void insertRow(Long dbId, String tableName, RowDTO request);
    void updateRow(Long dbId, String tableName, RowDTO request);
    void deleteRow(Long dbId, String tableName, RowDTO request);
    List<String> getTableColumns(Long dbId, String tableName);
}
