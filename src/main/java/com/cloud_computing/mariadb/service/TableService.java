package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.request.TableAlterRequest;
import com.cloud_computing.mariadb.dto.request.TableCreateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface TableService {
    void importSql(Long dbId, String sqlContent);
    void importSqlFile(Long dbId, MultipartFile file);
    void createTable(Long dbId, TableCreateRequest dto);
    public void alterTable(Long dbId, String tableName, TableAlterRequest request);
}
