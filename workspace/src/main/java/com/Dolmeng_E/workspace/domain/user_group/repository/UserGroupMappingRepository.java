package com.Dolmeng_E.workspace.domain.user_group.repository;

import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroupMapping;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGroupMappingRepository extends JpaRepository<UserGroupMapping, String> {
    int countByUserGroup(UserGroup userGroup);

    List<UserGroupMapping> findByUserGroup(UserGroup userGroup);

    Page<UserGroupMapping> findByUserGroup(UserGroup userGroup, Pageable pageable);

    void deleteByUserGroupAndWorkspaceParticipant_UserIdIn(UserGroup userGroup, List<UUID> userIds);

    List<UserGroupMapping> findAllByUserGroup(UserGroup userGroup);

    void deleteAllByUserGroup(UserGroup userGroup);

    boolean existsByWorkspaceParticipant(WorkspaceParticipant workspaceParticipant);

    void deleteByWorkspaceParticipant(WorkspaceParticipant participant);

    List<UserGroupMapping> findByUserGroupIn(List<UserGroup> groups);

    // 매핑 + 그룹을 한 번에(단건) 가져오기
    @Query("""
           select ugm
           from UserGroupMapping ugm
           join fetch ugm.userGroup ug
           where ugm.workspaceParticipant.id = :workspaceParticipantId
           """)
    Optional<UserGroupMapping> findOneByWorkspaceParticipantFetchGroup(String workspaceParticipantId);
}
