package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.dto.DbMemberDTO;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.DbMember;
import com.cloud_computing.mariadb.entity.DbUser;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.entity.enums.DbRole;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.DbMemberRepository;
import com.cloud_computing.mariadb.repository.DbRepository;
import com.cloud_computing.mariadb.repository.DbUserRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
import com.cloud_computing.mariadb.service.DbMemberService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired
    DbUserRepository dbUserRepository;
    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    JdbcTemplate mariadbJdbcTemplate;

    @Override
    public List<DbMemberDTO> getDbMembersByProjectId(Long dbId) {
        Db db = dbRepository.findById(dbId).orElseThrow(()->new RuntimeException("Không tìm thấy database."));
        User user = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));
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

    @Override
    @Transactional
    public void updateMemberRole(Long dbId, Long memberId, String role) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));
        DbMember currentMember = dbMemberRepository.findById(memberId).orElseThrow(() -> new UnauthorizedException("Bạn không có quyền truy cập vào database này."));
        if (!DbRole.OWNER.name().equals(currentMember.getRole()))
            throw new UnauthorizedException("Chỉ người sở hữu mới có quyền thay đổi role của thành viên.");
        DbRole newRole;
        try {
            newRole = DbRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Role không hợp lệ: " + role);
        }
        if (currentUser.getId().equals(currentMember.getUser().getId()))
            throw new BadRequestException("Bạn không thể thay đổi role của chính mình.");
        if (newRole == DbRole.OWNER)
            throw new BadRequestException("Không thể cấp role OWNER cho members khác.");
        DbMember targetMember = dbMemberRepository.findByDb_IdAndUser_Id(dbId, currentMember.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member không tồn tại trong database này."));
        Db db = dbRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database không tồn tại."));

        DbUser dbUser = dbUserRepository.findByUser_IdAndDb_Id(currentMember.getUser().getId(), dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user credential."));
        try{
            revokeAllPrivileges(db.getName(), dbUser.getUsername());
            String grantSql = newRole.getGrantStatement(db.getName(), dbUser.getUsername());
            mariadbJdbcTemplate.execute(grantSql);
            mariadbJdbcTemplate.execute("FLUSH PRIVILEGES");
            targetMember.setRole(newRole.name());
            dbMemberRepository.save(targetMember);
        }catch (Exception e){
            throw new RuntimeException("Không thể cập nhật role: " + e.getMessage());
        }
    }

    @Override
    public void deleteMember(Long dbId, Long memberId) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername()).orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập."));
        DbMember currentMember = dbMemberRepository.findByDb_IdAndUser_Id(dbId, currentUser.getId()).orElseThrow(() -> new UnauthorizedException("Bạn không có quyền truy cập database này."));
        if (!DbRole.OWNER.name().equals(currentMember.getRole()) &&
                !DbRole.ADMIN.name().equals(currentMember.getRole())) {
            throw new UnauthorizedException("Chỉ OWNER/ADMIN mới có quyền xóa members.");
        }
        if (currentUser.getId().equals(currentMember.getUser().getId())) {
            throw new BadRequestException("Bạn không thể xóa chính mình khỏi database. Hãy liên hệ OWNER.");
        }
        DbMember targetMember = dbMemberRepository.findByDb_IdAndUser_Id(dbId, currentMember.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member không tồn tại trong database này."));
        if (DbRole.OWNER.name().equals(targetMember.getRole())) {
            throw new BadRequestException("Không thể xóa OWNER khỏi database.");
        }
        if (DbRole.ADMIN.name().equals(currentMember.getRole()) &&
                DbRole.ADMIN.name().equals(targetMember.getRole())) {
            throw new BadRequestException("ADMIN không thể xóa ADMIN khác. Chỉ OWNER mới có quyền này.");
        }
        Db db = dbRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database không tồn tại."));

        DbUser dbUser = dbUserRepository.findByUser_IdAndDb_Id(currentMember.getUser().getId(), dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user credential."));
        try{
            revokeAllPrivileges(db.getName(), dbUser.getUsername());
            String dropUserSql = String.format("DROP USER IF EXISTS '%s'@'%%'", dbUser.getUsername());
            mariadbJdbcTemplate.execute(dropUserSql);
            mariadbJdbcTemplate.execute("FLUSH PRIVILEGES");

            dbUserRepository.delete(dbUser);

            dbMemberRepository.delete(targetMember);

        }catch (Exception e){
            throw new RuntimeException("Không thể xóa member: " + e.getMessage());
        }
    }


    private void revokeAllPrivileges(String dbName, String username) {
        try {
            String revokeSql = String.format(
                    "REVOKE ALL PRIVILEGES ON `%s`.* FROM '%s'@'%%'",
                    dbName, username
            );
            mariadbJdbcTemplate.execute(revokeSql);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
