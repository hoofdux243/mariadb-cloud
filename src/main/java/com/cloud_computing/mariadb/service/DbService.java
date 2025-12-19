package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.DbDTO;

import java.util.List;

public interface DbService {
    DbDTO createDb(DbDTO dbDTO);
    List<DbDTO> getDbsByProjectId(Long projectId);
}
