package com.Dolmeng_E.workspace.domain.task.controller;

import com.Dolmeng_E.workspace.domain.task.dto.TaskCreateDto;
import com.Dolmeng_E.workspace.domain.task.dto.TaskModifyDto;
import com.Dolmeng_E.workspace.domain.task.dto.TaskResDto;
import com.Dolmeng_E.workspace.domain.task.service.TaskService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PatchMapping("")
    public ResponseEntity<?> modifyTask(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody TaskModifyDto dto
    ) {
        String taskId = taskService.modifyTask(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("태스크 수정 완료")
                .result(taskId)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 태스크 삭제(삭제시 스톤의 task수 반영 필요)
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String taskId
    ) {
        taskService.deleteTask(userId, taskId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("태스크 삭제 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 태스크 완료 처리(완료시 스톤의 마일스톤 반영 필요)
    @PatchMapping("/done/{taskId}")
    public ResponseEntity<?> completeTask(@RequestHeader("X-User-Id") String userId,
                                          @PathVariable String taskId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("태스크 완료 처리 성공")
                .result("마일스톤: " + taskService.completeTask(userId, taskId) + "%")
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 태스크 목록 조회
    @GetMapping("/{stoneId}")
    public ResponseEntity<?> getTaskList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String stoneId
    ) {
        List<TaskResDto> dto = taskService.getTaskList(userId, stoneId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("태스크 목록 조회 성공")
                .result(dto)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }



}
