package com.Dolmeng_E.search.domain.search.repository;

import com.Dolmeng_E.search.domain.search.entity.StoneDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface StoneDocumentRepository extends ElasticsearchRepository<StoneDocument, String> {
}
