package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.AuditLogDTO;
import com.cloud_computing.mariadb.entity.AuditLog;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.AuditLogRepository;
import com.cloud_computing.mariadb.repository.DbMemberRepository;
import com.cloud_computing.mariadb.repository.DbRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
import com.cloud_computing.mariadb.service.AuditLogService;
import com.cloud_computing.mariadb.service.DbMemberService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Component
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final DbRepository dbRepository;
    private final DbMemberRepository dbMemberRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    @Transactional
    public void log(User user, Db db, String action, Map<String, Object> details) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            log(user, db, action, detailsJson);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional
    public void log(User user, Db db, String action, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .db(db)
                    .action(action)
                    .details(details)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Page<AuditLogDTO> getDbLogs(Long dbId, int page, int size) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));
        if(!dbMemberRepository.existsByDb_IdAndUser_Id(dbId, currentUser.getId())){
            throw new UnauthorizedException("Bạn không có quyền truy cập vào database này.");
        }
        Page<AuditLog> logs = auditLogRepository.findByDb_IdOrderByCreatedAtDesc(dbId, PageRequest.of(page,size));
        return logs.map(
                log -> AuditLogDTO.builder()
                        .id(log.getId())
                        .userId(log.getUser().getId())
                        .userName(log.getUser().getName())
                        .dbId(log.getDb().getId())
                        .dbName(log.getDb().getName())
                        .action(log.getAction())
                        .details(log.getDetails())
                        .createdAt(log.getCreatedAt())
                        .build()
        );
    }

    @Override
    public Page<AuditLogDTO> getUserLogs(Long userId, int page, int size) {
        return null;
    }

    @Override
    public Page<AuditLogDTO> getAllLogs(int page, int size) {
        return null;
    }
}
