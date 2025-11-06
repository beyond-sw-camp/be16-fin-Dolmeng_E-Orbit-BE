package com.Dolmeng_E.workspace.domain.user_group.repository;

import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, String> {
    boolean existsByWorkspaceAndUserGroupName(Workspace workspace, String userGroupName);
    Page<UserGroup> findByWorkspace(Workspace workspace, Pageable pageable);
    List<UserGroup> findByWorkspace(Workspace workspace);

    // 워크스페이스에서 사용자 그룹 이름을 포함한 정보 조회
    Page<UserGroup> findByWorkspaceAndUserGroupNameContainingIgnoreCase(
            Workspace workspace,
            String userGroupName,
            Pageable pageable
    );
}
