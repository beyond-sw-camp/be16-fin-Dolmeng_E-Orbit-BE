package com.Dolmeng_E.workspace.domain.access_group.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface AccessDetailRepository extends JpaRepository<AccessDetail,String> {
    List<AccessDetail> findByAccessGroup(AccessGroup accessGroup);
    void deleteAllByAccessGroup(AccessGroup accessGroup);
    Optional<AccessDetail> findByAccessGroupAndAccessListId(AccessGroup accessGroup, String accessListId);

    // AccessList까지 조인해서 한 번에 가져오도록 (N+1 방지)
    @Query("""
           select ad 
           from AccessDetail ad
           join fetch ad.accessList al
           where ad.accessGroup.id = :accessGroupId
           """)
    List<AccessDetail> findAllByAccessGroupIdWithAccessList(String accessGroupId);
}
