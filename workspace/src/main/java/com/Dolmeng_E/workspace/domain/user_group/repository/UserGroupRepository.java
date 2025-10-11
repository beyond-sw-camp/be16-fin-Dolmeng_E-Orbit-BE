package com.Dolmeng_E.workspace.domain.user_group.repository;

import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, String> {
    boolean existsByWorkspaceAndUserGroupName(Workspace workspace, String userGroupName);
    Page<UserGroup> findByWorkspace(Workspace workspace, Pageable pageable);
}
