package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {
    Optional<Document> findByFolderAndTitleAndIsDeleteFalse(Folder folder, String title);
    List<Document> findAllByFolderIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType rootType, String rootId);
    Optional<Document> findByRootIdAndTitleAndIsDeleteFalse(String rootId, String title);
}
