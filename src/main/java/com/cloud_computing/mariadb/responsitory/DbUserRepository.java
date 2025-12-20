package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.entity.DbUser;
import org.aspectj.apache.bcel.classfile.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DbUserRepository extends JpaRepository<DbUser, Long> {
    Optional<DbUser> findByUser_IdAndDb_Id(Long userId, Long dbId);
}
