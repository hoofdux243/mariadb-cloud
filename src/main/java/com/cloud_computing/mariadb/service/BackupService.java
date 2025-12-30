package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.BackupDTO;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;


public interface BackupService {
    BackupDTO createBackup(Long dbId, String description);
    Page<BackupDTO> getBackups(Long dbId, int page, int size);
    Resource downloadBackup(Long backupId);
    void deleteBackup(Long dbId, Long backupId);
    void restoreBackup(Long dbId, Long backupId);
    void importSqlDump(Long dbId, MultipartFile dumpFile);
}
