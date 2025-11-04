package com.Dolmeng_E.search.domain.search.repository;

import com.Dolmeng_E.search.domain.search.entity.DocumentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentDocumentRepository extends ElasticsearchRepository<DocumentDocument, String> {
    void deleteByRootTypeAndRootId(String rootType, String rootId);
}
