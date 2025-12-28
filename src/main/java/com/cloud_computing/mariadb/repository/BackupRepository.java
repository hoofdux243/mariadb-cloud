package com.cloud_computing.mariadb.repository;

import com.cloud_computing.mariadb.entity.Backup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackupRepository extends JpaRepository<Backup, Long> {
    Page<Backup> findByDb_IdOrderByCreatedAtDesc(Long dbId, Pageable pageable);
}
