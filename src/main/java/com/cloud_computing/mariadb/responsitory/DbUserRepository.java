package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.entity.DbUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbUserRepository extends JpaRepository<DbUser, Long> {
}
