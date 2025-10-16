package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.DocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentLineRepository extends JpaRepository<DocumentLine, Long> {
    List<DocumentLine> findAllDocumentLinesByDocumentId(String documentId);
}
