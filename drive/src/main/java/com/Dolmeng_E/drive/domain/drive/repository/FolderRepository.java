package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    Optional<Folder> findByParentIdAndNameAndIsDeleteIsFalse(String parentId, String name);
    List<Folder> findAllByParentIdAndIsDeleteIsFalse(String parentId);
    List<Folder> findAllByParentIdIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType rootType, String rootId);
}
