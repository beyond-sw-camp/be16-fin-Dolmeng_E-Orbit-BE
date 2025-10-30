package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.dto.FolderInfoDto;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    Optional<Folder> findByParentIdAndNameAndIsDeleteIsFalse(String parentId, String name);
    List<Folder> findAllByParentIdAndIsDeleteIsFalse(String parentId);
    List<Folder> findAllByParentIdIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType rootType, String rootId);
    @Query(value = """
        WITH RECURSIVE Ancestors AS (
            SELECT id, name, parent_id, is_delete 
            FROM folder
            WHERE id = (
                SELECT parent_id 
                FROM folder 
                WHERE id = :id AND is_delete = false
            )
            AND is_delete = false 
            
            UNION ALL
            SELECT f.id, f.name, f.parent_id, f.is_delete
            FROM folder f
            JOIN Ancestors a ON f.id = a.parent_id
            WHERE f.is_delete = false 
        )
        SELECT id AS folderId, name AS folderName FROM Ancestors;
    """, nativeQuery = true)
    List<FolderInfoDto> findAncestors(@Param("id") String id);
}
