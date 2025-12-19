package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.entity.Db;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbRepository extends JpaRepository<Db, Long> {
    Boolean existsByName(String name);
}
