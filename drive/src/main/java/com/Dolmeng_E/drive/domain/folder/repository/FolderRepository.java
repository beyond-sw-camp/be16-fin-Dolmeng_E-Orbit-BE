package com.Dolmeng_E.drive.domain.folder.repository;

import com.Dolmeng_E.drive.domain.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    Optional<Folder> findByParentIdAndNameAndIsDeleteIsFalse(String parentId, String name);
    List<Folder> findAllByParentIdAndIsDeleteIsFalse(String parentId);
}
