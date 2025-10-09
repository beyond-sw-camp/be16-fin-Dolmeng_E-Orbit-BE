package com.Dolmeng_E.workspace.domain.access_group.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccessGroupRepository extends JpaRepository<AccessGroup,String> {
    Optional<AccessGroup> findByWorkspaceIdAndAccessGroupName(String workspaceId, String accessGroupName);
    Boolean existsByWorkspaceIdAndAccessGroupName(String workspaceId, String accessGroupName);
    Page<AccessGroup> findByWorkspaceId(String workspaceId, Pageable pageable);
}
