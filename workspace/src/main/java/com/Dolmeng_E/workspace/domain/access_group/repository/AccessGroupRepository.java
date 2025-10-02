package com.Dolmeng_E.workspace.domain.access_group.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessGroupRepository extends JpaRepository<AccessGroup,String> {
}
