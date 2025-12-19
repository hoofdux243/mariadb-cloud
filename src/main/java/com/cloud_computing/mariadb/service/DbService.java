package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.DbDTO;

public interface DbService {
    DbDTO createDb(DbDTO dbDTO);
}
