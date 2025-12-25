package com.cloud_computing.mariadb.repository;

import com.cloud_computing.mariadb.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
