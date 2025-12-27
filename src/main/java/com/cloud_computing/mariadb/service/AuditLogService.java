package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.AuditLogDTO;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Map;


public interface AuditLogService {
    void log(User user, Db db, String action, Map<String, Object> details);
    void log(User user, Db db, String action, String details);
    Page<AuditLogDTO> getDbLogs(Long dbId, int page, int size);
    Page<AuditLogDTO> getUserLogs(Long userId, int page, int size);
    Page<AuditLogDTO> getAllLogs(int page, int size);}
