package com.Dolmeng_E.workspace.domain.stone.repository;

import com.Dolmeng_E.workspace.domain.stone.entity.ChildStoneList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildStoneListRepository extends JpaRepository<ChildStoneList,String> {
}
