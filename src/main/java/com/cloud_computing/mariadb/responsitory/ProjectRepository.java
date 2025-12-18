package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.dto.ProjectDTO;
import com.cloud_computing.mariadb.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByUser_IdAndName(Long userId, String name);
    Optional<Project> findByUser_UsernameAndId(String userUsername, Long id);
    List<Project> findAllByUser_Id(Long id);
}
