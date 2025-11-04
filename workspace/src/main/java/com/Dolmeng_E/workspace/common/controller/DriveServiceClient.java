package com.Dolmeng_E.workspace.common.controller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "drive-service")
public interface DriveServiceClient {

    @DeleteMapping("/drive/{rootType}/{rootId}/all")
    void deleteAll(@PathVariable String rootType, @PathVariable String rootId);
}