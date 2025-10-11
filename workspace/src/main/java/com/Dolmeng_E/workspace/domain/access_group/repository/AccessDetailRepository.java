package com.Dolmeng_E.workspace.domain.access_group.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface AccessDetailRepository extends JpaRepository<AccessDetail,String> {
    List<AccessDetail> findByAccessGroup(AccessGroup accessGroup);
    void deleteAllByAccessGroup(AccessGroup accessGroup);
}
