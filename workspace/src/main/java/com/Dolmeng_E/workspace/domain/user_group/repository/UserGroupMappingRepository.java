package com.Dolmeng_E.workspace.domain.user_group.repository;

import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroupMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserGroupMappingRepository extends JpaRepository<UserGroupMapping, String> {
    int countByUserGroup(UserGroup userGroup);

    List<UserGroupMapping> findByUserGroup(UserGroup userGroup);

    Page<UserGroupMapping> findByUserGroup(UserGroup userGroup, Pageable pageable);

    void deleteByUserGroupAndWorkspaceParticipant_UserIdIn(UserGroup userGroup, List<UUID> userIds);

    List<UserGroupMapping> findAllByUserGroup(UserGroup userGroup);

    void deleteAllByUserGroup(UserGroup userGroup);
}
