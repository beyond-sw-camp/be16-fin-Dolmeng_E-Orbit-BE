package com.Dolmeng_E.search.domain.search.repository;

import com.Dolmeng_E.search.domain.search.entity.TaskDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TaskDocumentRepository extends ElasticsearchRepository<TaskDocument, String> {
}
