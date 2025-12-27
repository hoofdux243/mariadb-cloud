package com.cloud_computing.mariadb.aspect;

import com.cloud_computing.mariadb.annotation.AuditLog;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.repository.DbRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
import com.cloud_computing.mariadb.service.AuditLogService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final DbRepository dbRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.cloud_computing.mariadb.annotation.AuditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. Execute method
        Object result = joinPoint.proceed();

        try {
            logAuditAsync(joinPoint);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return result;
    }
    private void logAuditAsync(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            AuditLog auditLog = signature.getMethod().getAnnotation(AuditLog.class);

            if (auditLog == null) {
                return;
            }
            String username = SecurityUtils.getUsername();
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                return; // Không log nếu không có user
            }

            // Extract dbId from method params
            Long dbId = extractDbId(joinPoint);
            Db db = dbId != null ? dbRepository.findById(dbId).orElse(null) : null;

            // Build details
            Map<String, Object> details = new HashMap<>();

            if (auditLog.includeParams()) {
                String[] paramNames = signature.getParameterNames();
                Object[] paramValues = joinPoint.getArgs();

                for (int i = 0; i < paramNames.length; i++) {
                    if (paramValues[i] != null && isPrimitiveOrString(paramValues[i])) {
                        details.put(paramNames[i], paramValues[i]);
                    }
                }
            }

            if (!auditLog.description().isEmpty()) {
                details.put("description", auditLog.description());
            }

            // Log
            auditLogService.log(user, db, auditLog.action(), details);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Tìm dbId trong params
     */
    private Long extractDbId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if ("dbId".equals(paramNames[i]) && paramValues[i] instanceof Long) {
                return (Long) paramValues[i];
            }
        }

        return null;
    }

    /**
     * Check nếu là primitive hoặc String
     */
    private boolean isPrimitiveOrString(Object obj) {
        return obj instanceof String ||
                obj instanceof Number ||
                obj instanceof Boolean ||
                obj.getClass().isPrimitive();
    }
}

