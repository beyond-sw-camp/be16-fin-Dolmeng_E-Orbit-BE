package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String> {
    Optional<Document> findByFolderAndTitleAndIsDeleteFalseAndRootId(Folder folder, String title, String rootId);
    Optional<Document> findByFolderAndTitleAndIsDeleteFalse(Folder folder, String title);
    List<Document> findAllByFolderIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType rootType, String rootId);
    Optional<Document> findByRootIdAndTitleAndIsDeleteFalse(String rootId, String title);

    @Modifying(clearAutomatically = true) // (중요) 쿼리 실행 후 1차 캐시(영속성 컨텍스트)를 클리어합니다.
    @Transactional // (중요) 업데이트/삭제 쿼리는 트랜잭션 내에서 실행되어야 합니다.
    @Query("UPDATE Document e SET e.isDelete = true WHERE e.rootType = :rootType AND e.rootId = :rootId")
    void softDeleteByRootInfo(@Param("rootType") RootType rootType, @Param("rootId") String rootId);
}
