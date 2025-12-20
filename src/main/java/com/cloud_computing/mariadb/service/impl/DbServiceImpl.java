package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.DbDTO;
import com.cloud_computing.mariadb.entity.*;
import com.cloud_computing.mariadb.entity.enums.DbRole;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.responsitory.*;
import com.cloud_computing.mariadb.service.DbService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

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
        if(!projectRepository.existsByUser_UsernameAndId(SecurityUtils.getUsername(), request.getProjectId())){
            throw new UnauthorizedException("Không có quyền tạo database trong project của người khác.");
        }
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));
        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project"));
        if(dbRepository.existsByName(request.getName()))
            throw new BadRequestException("Tên database đã tồn tại");
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
                .role(DbRole.OWNER.name())
                .build());

        return DbDTO.builder()
                .id(db.getId())
                .name(db.getName())
                .projectId(project.getId())
                .projectName(project.getName())
                .createdAt(db.getCreatedAt())
                .credentialInfo(DbDTO.CredentialInfo.builder()
                        .hostname(hostname)
                        .port(port)
                        .username(username)
                        .password(password)
                        .connectionString(buildConnectionString(hostname, port, request.getName(), username, password))
                        .build())
                .build();
    }

    @Override
    public List<DbDTO> getDbsByProjectId(Long projectId) {
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new BadRequestException("Không tìm thấy user."));
        List<Db> dbs = dbRepository.findAllByProject_Id(projectId);
        return dbs.stream().map(
                db -> {
                    return DbDTO.builder()
                            .id(db.getId())
                            .name(db.getName())
                            .createdAt(db.getCreatedAt())
                            .build();
                }
        ).collect(Collectors.toList());
    }

    @Override
    public List<DbDTO> getDbs() {
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new BadRequestException("Không tìm thấy user."));
        List<DbMember> dbms = dbMemberRepository.findAllByUser_Id(user.getId());
        return dbms.stream()
                .map(dbm ->{
                    return DbDTO.builder()
                            .id(dbm.getDb().getId())
                            .name(dbm.getDb().getName())
                            .projectName(dbm.getDb().getProject().getName())
                            .createdAt(dbm.getDb().getCreatedAt())
                            .build();
        }).collect(Collectors.toList());
    }

    @Override
    public DbDTO getDb(Long id) {
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new BadRequestException("Không tìm thấy user."));
        DbMember dbm = dbMemberRepository.findByIdAndUser_Id(id,user.getId()).orElseThrow(() -> new BadRequestException("Không tìm thấy database."));
        DbUser dbu = dbUserRepository.findByUser_IdAndDb_Id(user.getId(),id).orElseThrow(() -> new UnauthorizedException("Không tìm thấy database credential."));

        return DbDTO.builder()
                .id(id)
                .name(dbm.getDb().getName())
                .projectName(dbm.getDb().getProject().getName())
                .createdAt(dbm.getDb().getCreatedAt())
                .credentialInfo(DbDTO.CredentialInfo.builder()
                        .hostname(dbm.getDb().getHostname())
                        .port(dbm.getDb().getPort())
                        .username(dbu.getUsername())
                        .password(dbu.getPassword())
                        .connectionString(buildConnectionString(dbu.getDb().getHostname()
                                                                ,dbu.getDb().getPort()
                                                                ,dbu.getDb().getName()
                                                                ,dbu.getUsername()
                                                                ,dbu.getPassword()))
                        .build())
                .build();
    }

    @Override
    @Transactional
    public void deleteDb(Long id) {
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new BadRequestException("Không tìm thấy user"));
        Db db = dbRepository.findById(id).orElseThrow(() -> new BadRequestException("Không tìm thấy database."));

        DbMember dbm = dbMemberRepository.findByIdAndUser_Id(id, user.getId()).orElseThrow(() -> new UnauthorizedException("Bạn không có quyền truy cập database này."));
        if (!DbRole.OWNER.name().equals(dbm.getRole()))
            throw new UnauthorizedException("Bạn không có quyền xóa database này.");

        if (!dbRepository.existsById(db.getId()))
            throw new BadRequestException("Database không tồn tại.");

        try {
            dropDatabaseOnMariaDb(db.getName());
            dropAllUsersOnMariaDB(db.getName());
            List<DbMember> dbms = dbMemberRepository.findAllByDb_Id(id);
            dbMemberRepository.deleteAll(dbms);

            List<DbUser> dbus = dbUserRepository.findAllByDb_Id(id);
            dbUserRepository.deleteAll(dbus);
            dbRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void dropDatabaseOnMariaDb(String dbName){
        try {
            String sql = String.format("DROP DATABASE IF EXISTS `%s`", dbName);
            mariadbJdbcTemplate.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    private void dropAllUsersOnMariaDB(String dbName) {
        try {
            String getUsersSql = String.format(
                    "SELECT DISTINCT User FROM mysql.db WHERE Db = '%s'", dbName.replace("'", "''")
            );
            List<String> users = mariadbJdbcTemplate.queryForList(getUsersSql, String.class);
            if (users.isEmpty()) {
                return;
            }
            for (String user : users) {
                try {
                    String dropUserSql = String.format("DROP USER IF EXISTS '%s'@'%%'", user);
                    mariadbJdbcTemplate.execute(dropUserSql);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            mariadbJdbcTemplate.execute("FLUSH PRIVILEGES");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

//
//    private void revokeAllPrivileges(String dbName) {
//        try {
//            String getUsersSql = String.format(
//                    "SELECT DISTINCT User FROM mysql.db WHERE Db = '%s'",
//                    dbName
//            );
//            List<String> users = mariadbJdbcTemplate.queryForList(getUsersSql, String.class);
//
//            for (String user : users) {
//                String revokeSql = String.format(
//                        "REVOKE ALL PRIVILEGES ON `%s`.* FROM '%s'@'%%'",
//                        dbName, user
//                );
//                mariadbJdbcTemplate.execute(revokeSql);
//            }
//            mariadbJdbcTemplate.execute("FLUSH PRIVILEGES");
//        } catch (Exception e) {
//            throw new RuntimeException("Revoke quyền không thành công: " + e.getMessage());
//        }
//
//    }

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
