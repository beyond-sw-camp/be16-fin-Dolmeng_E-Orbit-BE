package com.Dolmeng_E.workspace.common.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "search-service")
public interface SearchServiceClient {

    @DeleteMapping("/{rootType}/{rootId}/all")
    void deleteAll(@PathVariable String rootType, @PathVariable String rootId);
}