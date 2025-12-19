package com.cloud_computing.mariadb.responsitory;

import com.cloud_computing.mariadb.entity.DbMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbMemberRepository extends JpaRepository<DbMember, Long> {
}
