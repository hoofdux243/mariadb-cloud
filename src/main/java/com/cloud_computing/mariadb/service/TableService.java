package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.request.TableAlterRequest;
import com.cloud_computing.mariadb.dto.request.TableCreateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TableService {
    void createTable(Long dbId, TableCreateRequest dto);
    void alterTable(Long dbId, String tableName, TableAlterRequest request);
    void renameTable(Long dbId, String oldName, String newName);
    void dropTable(Long dbId, String tableName);
    List<String> getTables(Long dbId);
    public Map<String, Object> getTableStructure(Long dbId, String tableName);
}
