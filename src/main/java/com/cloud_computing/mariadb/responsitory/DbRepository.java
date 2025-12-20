package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.entity.Db;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DbRepository extends JpaRepository<Db, Long> {
    Boolean existsByName(String name);
    Boolean existsByProject_Id(Long projectId);
    List<Db> findAllByProject_Id(Long projectId);
}
