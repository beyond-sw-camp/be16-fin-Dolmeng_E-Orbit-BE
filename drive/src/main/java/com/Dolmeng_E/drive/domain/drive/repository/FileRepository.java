package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.File;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, String> {
    Optional<File> findByFolderAndNameAndIsDeleteFalseAndRootId(Folder folder, String name, String rootId);
    Optional<File> findByFolderAndNameAndIsDeleteFalse(Folder folder, String name);

    @Query("SELECT SUM(f.size) FROM File f WHERE f.workspaceId = :workspaceId")
    Long findTotalSizeByWorkspaceId(@Param("workspaceId") String workspaceId);

    List<File> findAllByFolderIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType rootType, String rootId);

    Optional<File> findByRootIdAndNameAndIsDeleteFalse(String rootId, String name);
}
