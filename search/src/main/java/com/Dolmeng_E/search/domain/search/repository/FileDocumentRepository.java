package com.Dolmeng_E.search.domain.search.repository;

import com.Dolmeng_E.search.domain.search.entity.FileDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FileDocumentRepository extends ElasticsearchRepository<FileDocument, String> {
    void deleteByRootTypeAndRootId(String rootType, String rootId);
}
