package com.cloud_computing.mariadb.repository;

import com.cloud_computing.mariadb.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByDb_IdOrderByCreatedAtDesc(Long dbId, Pageable pageable);
    void deleteAllByDb_Id(Long dbId);
}
