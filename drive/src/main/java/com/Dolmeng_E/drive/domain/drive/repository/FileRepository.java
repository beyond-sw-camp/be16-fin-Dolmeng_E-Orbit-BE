package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.File;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, String> {
    Optional<File> findByFolderAndNameAndIsDeleteFalseAndRootId(Folder folder, String name, String rootId);
    Optional<File> findByFolderAndNameAndIsDeleteFalse(Folder folder, String name);

    @Query("SELECT SUM(f.size) FROM File f WHERE f.workspaceId = :workspaceId")
    Long findTotalSizeByWorkspaceId(@Param("workspaceId") String workspaceId);

    List<File> findAllByFolderIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType rootType, String rootId);

    Optional<File> findByRootIdAndNameAndIsDeleteFalse(String rootId, String name);

    @Modifying(clearAutomatically = true) // (중요) 쿼리 실행 후 1차 캐시(영속성 컨텍스트)를 클리어합니다.
    @Transactional // (중요) 업데이트/삭제 쿼리는 트랜잭션 내에서 실행되어야 합니다.
    @Query("UPDATE File e SET e.isDelete = true WHERE e.rootType = :rootType AND e.rootId = :rootId")
    void softDeleteByRootInfo(@Param("rootType") RootType rootType, @Param("rootId") String rootId);
}
