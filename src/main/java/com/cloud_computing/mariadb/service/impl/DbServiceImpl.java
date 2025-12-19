package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.DbDTO;
import com.cloud_computing.mariadb.entity.*;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.responsitory.*;
import com.cloud_computing.mariadb.service.DbService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DbServiceImpl implements DbService {
    @Autowired
    DbRepository dbRepository;
    @Autowired
    DbUserRepository dbUserRepository;
    @Autowired
    DbMemberRepository dbMemberRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 16;

    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    JdbcTemplate mariadbJdbcTemplate;

    @Value("${spring.datasource.secondary.jdbc-url}")
    String mariadbUrl;

    @Override
    @Transactional
    public DbDTO createDb(DbDTO request) {
        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project"));
        if(dbRepository.existsByName(request.getName()))
            throw new BadRequestException("Database đã tồn tại");
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new BadRequestException("Không tìm thấy user"));

        String hostname = extractHostname(mariadbUrl);
        Integer port = extractPort(mariadbUrl);
        createDbOnMariaDb(request.getName());

        String username = String.format("%s_%s", request.getName(), user.getUsername());
        String password = generateRandomPassword();
        createUserOnMariaDb(request.getName(), username, password);
        Db db = dbRepository.save(Db.builder()
                .project(project)
                .name(request.getName())
                    .hostname(hostname)
                    .port(port)
                    .status("ACTIVE")
                .build());

        DbUser dbUser = dbUserRepository.save(DbUser.builder()
                .db(db)
                .user(user)
                .username(username)
                .password(password)
                .build());

        DbMember dbMember = dbMemberRepository.save(DbMember.builder()
                .db(db)
                .user(user)
                .role("OWNER")
                .build());

        return DbDTO.builder()
                .id(db.getId())
                .name(db.getName())
                .projectId(project.getId())
                .projectName(project.getName())
                .hostname(hostname)
                .port(port)
                .createdAt(db.getCreatedAt())
                .credentialInfo(DbDTO.CredentialInfo.builder()
                        .username(username)
                        .password(password)
                        .connectionString(buildConnectionString(hostname, port, request.getName(), username, password))
                        .build())
                .build();
    }

    private void createDbOnMariaDb(String dbName){
        try {
            String sql = String.format("CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci", dbName);
            mariadbJdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void createUserOnMariaDb(String dbName, String username, String password){
        try {
            mariadbJdbcTemplate.execute(String.format("CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s'", username, password));
            mariadbJdbcTemplate.execute(String.format("GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%%'", dbName, username));
            mariadbJdbcTemplate.execute("FLUSH PRIVILEGES");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
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

    private String buildConnectionString(String host, int port, String dbName, String username, String password) {
        return String.format("jdbc:mariadb://%s:%d/%s?user=%s&password=%s", host, port, dbName, username, password);
    }
}
