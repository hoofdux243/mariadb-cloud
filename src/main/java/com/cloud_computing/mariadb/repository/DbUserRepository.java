package com.cloud_computing.mariadb.repository;

import com.cloud_computing.mariadb.entity.DbUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DbUserRepository extends JpaRepository<DbUser, Long> {
    Optional<DbUser> findByUser_IdAndDb_Id(Long userId, Long dbId);
    List<DbUser> findAllByUser_Id(Long userId);
    List<DbUser> findAllByDb_Id(Long dbId);
}
