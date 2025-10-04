package com.Dolmeng_E.workspace.domain.access_group.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessList;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessListRepository extends JpaRepository<AccessList,String> {
    Optional<AccessList> findByAccessType(AccessType accessType);
}
