package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.dto.ProjectDTO;
import com.cloud_computing.mariadb.entity.Project;
import com.cloud_computing.mariadb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByUser_IdAndName(Long userId, String name);

    boolean existsByUser_UsernameAndId(String username, Long id);
    Optional<Project> findByUser_UsernameAndId(String username, Long id);
    List<Project> findAllByUser_IdOrderByCreatedAtAsc(Long id);
}
