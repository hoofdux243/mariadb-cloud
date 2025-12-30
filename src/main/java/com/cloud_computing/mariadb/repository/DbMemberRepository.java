package com.cloud_computing.mariadb.repository;

import com.cloud_computing.mariadb.entity.DbMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DbMemberRepository extends JpaRepository<DbMember, Long> {
    List<DbMember> findAllByUser_Id(Long userId);
    Optional<DbMember> findByDb_IdAndUser_Id(Long id, Long userId);
    List<DbMember> findAllByDb_Id(Long dbId);
    boolean existsByDb_IdAndUser_Id(Long dbId, Long userId);
    @Query("SELECT dm FROM DbMember dm " +
            "JOIN FETCH dm.user u " +
            "WHERE dm.db.id = :dbId")
    List<DbMember> findAllByDbIdWithUser(@Param("dbId") Long dbId);
    Long countByUser_Id(Long userId);
}
