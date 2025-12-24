package com.cloud_computing.mariadb.service;

import com.cloud_computing.mariadb.dto.DbMemberDTO;

import java.util.List;

public interface DbMemberService {
    List<DbMemberDTO> getDbMembersByProjectId(Long dbId);
    void updateMemberRole(Long dbId, Long memberId, String role);
    void deleteMember(Long dbId, Long memberId);
}
