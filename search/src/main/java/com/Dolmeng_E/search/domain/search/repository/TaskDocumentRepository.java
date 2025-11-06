package com.Dolmeng_E.search.domain.search.repository;

import com.Dolmeng_E.search.domain.search.entity.TaskDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface TaskDocumentRepository extends ElasticsearchRepository<TaskDocument, String> {
    void deleteByDocTypeAndId(String type, String id);
    List<TaskDocument> findAllByRootId(String rootId);
}
