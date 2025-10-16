package com.Dolmeng_E.workspace.domain.task.controller;

import com.Dolmeng_E.workspace.domain.task.dto.TaskCreateDto;
import com.Dolmeng_E.workspace.domain.task.service.TaskService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    // 태스크 생성(생성시 스톤의 task수 반영 필요)
    @PostMapping("")
    public ResponseEntity<?> createTask(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody TaskCreateDto dto
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("태스크 생성 완료")
                .result(taskService.createTask(userId, dto))
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }
    // 태스크 수정

    // 태스크 삭제(삭제시 스톤의 task수 반영 필요)

    // 태스크 완료 처리(완료시 스톤의 마일스톤 반영 필요)


}
