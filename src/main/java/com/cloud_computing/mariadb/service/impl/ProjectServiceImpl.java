package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.ProjectDTO;
import com.cloud_computing.mariadb.entity.Project;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.responsitory.ProjectRepository;
import com.cloud_computing.mariadb.responsitory.UserRepository;
import com.cloud_computing.mariadb.service.ProjectService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectServiceImpl implements ProjectService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;

    @Override
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (projectDTO.getName() == null || projectDTO.getName().trim().isEmpty()) {
            throw new BadRequestException("Tên database không được bỏ trống.");
        }
        if (projectRepository.existsByUser_IdAndName(user.getId(), projectDTO.getName())) {
            throw new BadRequestException("Tên project đã tồn tại.");
        }
        Project project = projectRepository.save(Project.builder()
                .user(user)
                .name(projectDTO.getName())
                .build());
        return ProjectDTO.builder()
                .id(project.getId())
                .userId(user.getId())
                .name(project.getName())
                .build();
    }

    @Override
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findByUser_UsernameAndId(SecurityUtils.getUsername(), projectId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy project."));
        projectRepository.delete(project);
    }

    @Override
    public List<ProjectDTO> getProjects() {
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        List<Project> projects = projectRepository.findAllByUser_IdOrderByCreatedAtAsc(user.getId());
        return projects.stream()
                .map(project -> {
                    return ProjectDTO.builder()
                            .id(project.getId())
                            .userId(project.getUser().getId())
                            .name(project.getName())
                            .createdAt(project.getCreatedAt())
                            .build();
                }).collect(Collectors.toList());
    }
}
