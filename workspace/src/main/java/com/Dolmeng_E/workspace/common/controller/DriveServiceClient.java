package com.Dolmeng_E.workspace.common.controller;

import com.example.modulecommon.dto.CommonSuccessDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "drive-service")
public interface DriveServiceClient {

    @DeleteMapping("/drive/{rootType}/{rootId}/all")
    void deleteAll(@PathVariable String rootType, @PathVariable String rootId);


    // 프로젝트 ID로 프로젝트 파일/문서 개수 + 파일 용량
    @GetMapping("/drive/elements/{projectId}")
    ResponseEntity<CommonSuccessDto> getElements(@PathVariable("projectId") String projectId);




}