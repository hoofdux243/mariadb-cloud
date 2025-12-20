package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.entity.DbMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DbMemberRepository extends JpaRepository<DbMember, Long> {
    List<DbMember> findByUser_Id(Long userId);
    Optional<DbMember> findByIdAndUser_Id(Long id, Long userId);
}
