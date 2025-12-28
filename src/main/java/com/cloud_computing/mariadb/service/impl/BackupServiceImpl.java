package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.annotation.AuditLog;
import com.cloud_computing.mariadb.dto.BackupDTO;
import com.cloud_computing.mariadb.entity.Backup;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.DbMember;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.entity.enums.DbRole;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.BackupRepository;
import com.cloud_computing.mariadb.repository.DbMemberRepository;
import com.cloud_computing.mariadb.repository.DbRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BackupServiceImpl implements BackupService {
    final BackupRepository backupRepository;
    final DbRepository dbRepository;
    final DbMemberRepository dbMemberRepository;
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

            // ✅ PARSE MARIADB URL để lấy host và port
            String host = extractHostname(mariadbUrl);
            Integer port = extractPort(mariadbUrl);


            // ✅ Thực hiện mysqldump kết nối tới MariaDB server
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-h", host,          // ← Host của MariaDB
                    "-P", String.valueOf(port),          // ← Port của MariaDB
                    "-u", mariadbUsername,  // ← Username MariaDB
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    db.getName()         // ← Tên database user đã tạo
            );

            // ✅ Set password MariaDB
            Map<String, String> env = pb.environment();
            env.put("MYSQL_PWD", mariadbPassword);

            pb.redirectOutput(tempFile.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Đọc error stream
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

            // Upload lên S3
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
    public void deleteBackup(Long backupId) {
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
    private String extractHostname(String url){
        String[] parts = url.split("//")[1].split(":");
        return parts[0];
    }

    private Integer extractPort(String jdbcUrl) {
        String[] parts = jdbcUrl.split(":");
        String portPart = parts[parts.length - 1].split("/")[0];
        return Integer.parseInt(portPart);
    }
}
