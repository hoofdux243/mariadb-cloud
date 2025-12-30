package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.DashboardDTO;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.BackupRepository;
import com.cloud_computing.mariadb.repository.DbMemberRepository;
import com.cloud_computing.mariadb.repository.ProjectRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
import com.cloud_computing.mariadb.service.DashboardService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardServiceImpl implements DashboardService {
    final DbMemberRepository dbMemberRepository;
    final BackupRepository backupRepository;
    final ProjectRepository projectRepository;
    final UserRepository userRepository;

    @Override
    public DashboardDTO getDashboard() {
        User user = userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập"));

        Long userId = user.getId();

        long totalProjects = projectRepository.countByUser_Id(userId);

        long totalDatabases = dbMemberRepository.countByUser_Id(userId);

        long totalBackups = backupRepository.countByUser_Id(userId);

        return DashboardDTO.builder()
                .totalProjects(totalProjects)
                .totalDatabases(totalDatabases)
                .totalBackups(totalBackups)
                .build();
    }
}
