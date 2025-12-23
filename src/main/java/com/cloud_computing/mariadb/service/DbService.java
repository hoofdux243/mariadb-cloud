package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.DbDTO;
import com.cloud_computing.mariadb.dto.DbMemberDTO;

import java.util.List;

public interface DbService {
    DbDTO createDb(DbDTO dbDTO);
    List<DbDTO> getDbsByProjectId(Long projectId);
    List<DbDTO> getDbs();
    DbDTO getDb(Long id);
    void deleteDb(Long id);
    void sendInvitation(Long dbId, DbMemberDTO dbMemberDTO);
    void acceptInvitation(String token);

}
