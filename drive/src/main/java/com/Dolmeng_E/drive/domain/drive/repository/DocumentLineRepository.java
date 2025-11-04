package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.DocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentLineRepository extends JpaRepository<DocumentLine, Long> {
    List<DocumentLine> findAllDocumentLinesByDocumentId(String documentId);
    Optional<DocumentLine> findByLineId(String lineId);
    Optional<DocumentLine> findByPrevId(String prevId);

    void deleteAllByDocument(Document document);
}
