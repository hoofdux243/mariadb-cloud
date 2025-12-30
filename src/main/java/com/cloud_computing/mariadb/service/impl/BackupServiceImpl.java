package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.annotation.AuditLog;
import com.cloud_computing.mariadb.dto.BackupDTO;
import com.cloud_computing.mariadb.entity.*;
import com.cloud_computing.mariadb.entity.enums.DbRole;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.*;
import com.cloud_computing.mariadb.service.BackupService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BackupServiceImpl implements BackupService {
    final BackupRepository backupRepository;
    final DbRepository dbRepository;
    final DbMemberRepository dbMemberRepository;
    final DbUserRepository dbUserRepository;
    final UserRepository userRepository;
    final S3Client s3Client;
    @Value("${spring.datasource.secondary.jdbc-url}")
    String mariadbUrl;
    @Value("${spring.datasource.secondary.username}")
    String mariadbUsername;
    @Value("${spring.datasource.secondary.password}")
    String mariadbPassword;
    @Value("${aws.s3.bucket-name}")
    String bucketName;

    @Value("${backup.temp-dir}")
    String tempDir;


    @Override
    @Transactional
    @AuditLog(action = "CREATE_BACKUP", description = "tạo backup")
    public BackupDTO createBackup(Long dbId, String description) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));
        DbMember member = dbMemberRepository.findByDb_IdAndUser_Id(dbId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền backup database này."));
        if (!DbRole.OWNER.name().equals(member.getRole()) &&
                !DbRole.ADMIN.name().equals(member.getRole())) {
            throw new UnauthorizedException("Chỉ OWNER/ADMIN mới có quyền backup.");
        }
        Db db = dbRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database không tồn tại."));
        Path tempFile = null;
        try {
            Files.createDirectories(Paths.get(tempDir));

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("%s_%s.sql", db.getName(), timestamp);
            String s3Key = String.format("backups/%s/%s", db.getName(), fileName);

            tempFile = Paths.get(tempDir, fileName);

            String host = extractHostname(mariadbUrl);
            Integer port = extractPort(mariadbUrl);


            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-h", host,
                    "-P", String.valueOf(port),
                    "-u", mariadbUsername,
                    "-p" + mariadbPassword,

                    "--skip-column-statistics",
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    "--events",
                    "--add-drop-table",
                    "--complete-insert",
                    "--hex-blob",
                    "--default-character-set=utf8mb4",

                    db.getName()
            );

            Map<String, String> env = pb.environment();
            env.put("MYSQL_PWD", mariadbPassword);

            pb.redirectOutput(tempFile.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorLog = errorOutput.toString();
                if (errorLog.isEmpty()) {
                    errorLog = "No error output";
                }
                throw new RuntimeException("mysqldump failed with exit code: " + exitCode +
                        "\nError: " + errorLog);
            }

            if (!Files.exists(tempFile) || Files.size(tempFile) == 0) {
                throw new RuntimeException("Backup file not created or empty");
            }

            long fileSize = Files.size(tempFile);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/sql")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(tempFile));

            // Lưu backup
            Backup backup = backupRepository.save(Backup.builder()
                    .db(db)
                    .user(currentUser)
                    .fileName(fileName)
                    .s3Key(s3Key)
                    .fileSize(fileSize)
                    .description(description)
                    .build());


            return toDTO(backup);

        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo backup: " + e.getMessage());
        } finally {
            // Xóa file temp
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi xóa temp file: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public Page<BackupDTO> getBackups(Long dbId, int page, int size) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));

        DbMember member = dbMemberRepository.findByDb_IdAndUser_Id(dbId, currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền xem backups."));

        Page<Backup> backups = backupRepository.findByDb_IdOrderByCreatedAtDesc(
                dbId, PageRequest.of(page, size));

        return backups.map(this::toDTO);
    }

    @Override
    public Resource downloadBackup(Long backupId) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));

        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup không tồn tại."));

        DbMember member = dbMemberRepository.findByDb_IdAndUser_Id(
                        backup.getDb().getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền download backup này."));

        try {
            // Download từ S3
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(backup.getS3Key())
                    .build();

            InputStream inputStream = s3Client.getObject(getRequest);

            return new InputStreamResource(inputStream);

        } catch (S3Exception e) {
            throw new ResourceNotFoundException("File backup không tồn tại trên S3.");
        }
    }

    @Override
    @Transactional
    @AuditLog(action = "DELETE_BACKUP", description = "Deleted backup")
    public void deleteBackup(Long dbId, Long backupId) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));

        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup không tồn tại."));

        DbMember member = dbMemberRepository.findByDb_IdAndUser_Id(
                        backup.getDb().getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền xóa backup này."));

        if (!DbRole.OWNER.name().equals(member.getRole()) &&
                !DbRole.ADMIN.name().equals(member.getRole())) {
            throw new UnauthorizedException("Chỉ OWNER/ADMIN mới có quyền xóa backup.");
        }

        try {
            // Xóa file trên S3
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(backup.getS3Key())
                    .build();

            s3Client.deleteObject(deleteRequest);

            // Xóa record
            backupRepository.delete(backup);
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa backup: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @AuditLog(action = "RESTORE_BACKUP", description = "Restored database from backup")
    public void restoreBackup(Long dbId, Long backupId) {
        User currentUser = getCurrentUser();

        // Lấy backup info
        Backup backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup không tồn tại"));

        Db db = backup.getDb();

        // Check permission (chỉ OWNER/ADMIN)
        checkPermission(db.getId(), currentUser, DbRole.ADMIN);

        DbUser dbUser = getDbUser(currentUser.getId(), db.getId());

        try {
            // ✅ DOWNLOAD FILE TỪ S3 DÙNG S3Client CÓ SẴN
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(backup.getS3Key())
                    .build();

            InputStream inputStream = s3Client.getObject(getRequest);

            // Restore vào database
            executeSqlFile(db, dbUser, inputStream);


        } catch (S3Exception e) {
            throw new ResourceNotFoundException("File backup không tồn tại trên S3");
        } catch (Exception e) {
            throw new RuntimeException("Restore thất bại: " + e.getMessage());
        }
    }

    /**
     * ✅ EXECUTE SQL FILE STREAM
     */
    private void executeSqlFile(Db db, DbUser dbUser, InputStream inputStream) throws IOException {
        // ✅ CONNECT VÀO DATABASE CỤ THỂ
        String url = String.format("jdbc:mariadb://%s:%d/%s?allowMultiQueries=true",
                db.getHostname(), db.getPort(), db.getName());

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(dbUser.getUsername());
        dataSource.setPassword(dbUser.getPassword());

        JdbcTemplate template = new JdbcTemplate(dataSource);


        // ✅ 1. TẮT FOREIGN KEY CHECKS
        template.execute("SET FOREIGN_KEY_CHECKS = 0");

        // ✅ 2. XÓA TẤT CẢ OBJECTS HIỆN CÓ (TABLES, PROCEDURES, FUNCTIONS, TRIGGERS, EVENTS)
        dropAllDatabaseObjects(template, db.getName());

        // ✅ 3. RESTORE TỪ FILE BACKUP
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        StringBuilder sqlBuilder = new StringBuilder();
        String line;
        int executedStatements = 0;
        int failedStatements = 0;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Skip comments và empty lines
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }

            // Xử lý /*!... */
            if (line.startsWith("/*!")) {
                String processed = line.replaceAll("/\\*!\\d+\\s*", "").replaceAll("\\s*\\*/;?$", ";");
                if (processed.trim().isEmpty() || processed.trim().equals(";")) {
                    continue;
                }
                line = processed;
            }

            sqlBuilder.append(line).append(" ");

            if (line.endsWith(";")) {
                String sql = sqlBuilder.toString().trim();

                try {
                    template.execute(sql);
                    executedStatements++;
                } catch (Exception e) {
                    failedStatements++;
                }

                sqlBuilder.setLength(0);
            }
        }
        template.execute("SET FOREIGN_KEY_CHECKS = 1");
        reader.close();
        List<String> newTables = template.queryForList("SHOW TABLES", String.class);
    }


    private JdbcTemplate createJdbcTemplate(Db db, DbUser dbUser) {
        String url = String.format("jdbc:mariadb://%s:%d/%s?allowMultiQueries=true",
                db.getHostname(), db.getPort(), db.getName());

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(dbUser.getUsername());
        dataSource.setPassword(dbUser.getPassword());

        return new JdbcTemplate(dataSource);
    }


    private void dropAllDatabaseObjects(JdbcTemplate template, String dbName) {
        try {

            List<String> tables = template.queryForList("SHOW TABLES", String.class);

            for (String tableName : tables) {
                try {
                    template.execute("DROP TABLE IF EXISTS `" + tableName + "`");
                } catch (Exception e) {
                }
            }

            List<Map<String, Object>> procedures = template.queryForList(
                    "SELECT ROUTINE_NAME FROM information_schema.ROUTINES " +
                            "WHERE ROUTINE_SCHEMA = ? AND ROUTINE_TYPE = 'PROCEDURE'",
                    dbName);


            for (Map<String, Object> proc : procedures) {
                String procName = (String) proc.get("ROUTINE_NAME");
                try {
                    template.execute("DROP PROCEDURE IF EXISTS `" + procName + "`");
                } catch (Exception e) {
                }
            }

            List<Map<String, Object>> functions = template.queryForList(
                    "SELECT ROUTINE_NAME FROM information_schema.ROUTINES " +
                            "WHERE ROUTINE_SCHEMA = ? AND ROUTINE_TYPE = 'FUNCTION'",
                    dbName);


            for (Map<String, Object> func : functions) {
                String funcName = (String) func.get("ROUTINE_NAME");
                try {
                    template.execute("DROP FUNCTION IF EXISTS `" + funcName + "`");
                } catch (Exception e) {
                }
            }

            List<Map<String, Object>> triggers = template.queryForList(
                    "SELECT TRIGGER_NAME FROM information_schema.TRIGGERS " +
                            "WHERE TRIGGER_SCHEMA = ?",
                    dbName);


            for (Map<String, Object> trig : triggers) {
                String trigName = (String) trig.get("TRIGGER_NAME");
                try {
                    template.execute("DROP TRIGGER IF EXISTS `" + trigName + "`");
                } catch (Exception e) {
                }
            }

            List<Map<String, Object>> events = template.queryForList(
                    "SELECT EVENT_NAME FROM information_schema.EVENTS " +
                            "WHERE EVENT_SCHEMA = ?",
                    dbName);


            for (Map<String, Object> event : events) {
                String eventName = (String) event.get("EVENT_NAME");
                try {
                    template.execute("DROP EVENT IF EXISTS `" + eventName + "`");
                } catch (Exception e) {
                }
            }


        } catch (Exception e) {
            throw new RuntimeException("Cannot drop database objects: " + e.getMessage());
        }
    }


    private void checkPermission(Long dbId, User user, DbRole minRole) {
        DbMember member = dbMemberRepository.findByDb_IdAndUser_Id(dbId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền truy cập database này"));

        DbRole userRole = DbRole.valueOf(member.getRole());

        if (minRole == DbRole.ADMIN) {
            if (userRole != DbRole.OWNER && userRole != DbRole.ADMIN) {
                throw new UnauthorizedException("Chỉ OWNER/ADMIN mới có quyền restore");
            }
        } else if (minRole == DbRole.READWRITE) {
            if (userRole != DbRole.OWNER && userRole != DbRole.ADMIN && userRole != DbRole.READWRITE) {
                throw new UnauthorizedException("Bạn không có quyền thực hiện thao tác này");
            }
        }
    }


    private User getCurrentUser() {
        return userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập"));
    }


    private DbUser getDbUser(Long userId, Long dbId) {
        return dbUserRepository.findByUser_IdAndDb_Id(userId, dbId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy credentials"));
    }


    private BackupDTO toDTO(Backup backup) {
        return BackupDTO.builder()
                .id(backup.getId())
                .dbId(backup.getDb().getId())
                .dbName(backup.getDb().getName())
                .userId(backup.getUser().getId())
                .userName(backup.getUser().getName())
                .description(backup.getDescription())
                .fileName(backup.getFileName())
                .fileSize(backup.getFileSize())
                .createdAt(backup.getCreatedAt())
                .build();
    }

    private String extractHostname(String url) {
        String[] parts = url.split("//")[1].split(":");
        return parts[0];
    }

    private Integer extractPort(String jdbcUrl) {
        String[] parts = jdbcUrl.split(":");
        String portPart = parts[parts.length - 1].split("/")[0];
        return Integer.parseInt(portPart);
    }
}
