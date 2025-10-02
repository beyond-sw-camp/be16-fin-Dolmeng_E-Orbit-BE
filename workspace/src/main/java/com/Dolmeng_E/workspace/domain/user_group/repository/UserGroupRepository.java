package com.Dolmeng_E.workspace.domain.user_group.repository;

import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
}
