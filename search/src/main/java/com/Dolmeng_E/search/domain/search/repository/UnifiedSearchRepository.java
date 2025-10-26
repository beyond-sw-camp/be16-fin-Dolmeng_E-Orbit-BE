package com.Dolmeng_E.search.domain.search.repository;

import com.Dolmeng_E.search.domain.search.entity.UnifiedSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UnifiedSearchRepository extends ElasticsearchRepository<UnifiedSearchDocument, String> {
    List<UnifiedSearchDocument> findBySearchTitle(String title);
}
