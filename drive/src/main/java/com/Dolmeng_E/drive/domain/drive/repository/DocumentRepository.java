package com.Dolmeng_E.drive.domain.drive.repository;

import com.Dolmeng_E.drive.domain.drive.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {
}
