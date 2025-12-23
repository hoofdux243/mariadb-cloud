package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.DbMemberDTO;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.DbMember;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.DbMemberRepository;
import com.cloud_computing.mariadb.repository.DbRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
import com.cloud_computing.mariadb.service.DbMemberService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DbMemberServiceImpl implements DbMemberService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    DbRepository dbRepository;
    @Autowired
    DbMemberRepository dbMemberRepository;

    @Override
    public List<DbMemberDTO> getDbMembersByProjectId(Long dbId) {
        Db db = dbRepository.findById(dbId).orElseThrow(()->new RuntimeException("Không tìm thấy database."));
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new BadRequestException("Không tìm thấy username."));
        if(!dbMemberRepository.existsByDb_IdAndUser_Id(dbId, user.getId()))
            throw new UnauthorizedException("Bạn không có quyền xem thành viên của database này.");
        List<DbMember> dbMembers = dbMemberRepository.findAllByDbIdWithUser(dbId);
        return dbMembers.stream().map(
                dbm -> {
                    return DbMemberDTO.builder()
                            .id(dbm.getId())
                            .userId(dbm.getUser().getId())
                            .username(dbm.getUser().getUsername())
                            .name(dbm.getUser().getName())
                            .email(dbm.getUser().getEmail())
                            .role(dbm.getRole())
                            .createdAt(dbm.getCreatedAt())
                            .build();
                }
        ).collect(Collectors.toList());
    }
}
