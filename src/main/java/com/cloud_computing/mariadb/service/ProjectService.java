package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.ProjectDTO;

import java.util.List;

public interface ProjectService {
    ProjectDTO createProject(ProjectDTO projectDTO);
    void deleteProject(Long projectId);
    List<ProjectDTO> getProjects();
}
