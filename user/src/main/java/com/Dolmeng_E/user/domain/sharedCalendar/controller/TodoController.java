package com.Dolmeng_E.user.domain.sharedCalendar.controller;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.*;
import com.Dolmeng_E.user.domain.sharedCalendar.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/todo")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    // todo 등록
    @PostMapping
    public TodoCreateResDto createTodo(@RequestHeader("X-User-Id") String userId,
                                       @RequestBody TodoCreateReqDto dto) {
        return todoService.createTodo(UUID.fromString(userId), dto);
    }

    // todo 특정 날짜 조회
    @GetMapping("/{workspaceId}")
    public List<TodoCreateResDto> getTodo(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return todoService.getTodo(UUID.fromString(userId), workspaceId, date);
    }

    // todo 전체 조회
    @GetMapping("/{workspaceId}/all")
    public List<TodoCreateResDto> getAllTodo(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        return todoService.getAllTodo(UUID.fromString(userId), workspaceId);
    }

    // todo 수정
    @PutMapping("/{todoId}")
    public TodoCreateResDto updateTodo(@RequestHeader("X-User-Id") String userId,
                                       @PathVariable String todoId,
                                       @RequestBody UpdateTodoReqDto dto) {
        return todoService.updateTodo(todoId, UUID.fromString(userId), dto);
    }

    // todo 삭제
    @DeleteMapping("/{todoId}")
    public void deleteTodo(@RequestHeader("X-User-Id") String userId,
                      @PathVariable String todoId) {
        todoService.deleteTodo(todoId, UUID.fromString(userId));
    }

    // todo 완료 처리
    @PutMapping("/completion/{todoId}")
    public TodoCreateResDto completedTodo(@RequestHeader("X-User-Id") String userId,
                                          @PathVariable String todoId,
                                          @RequestBody(required = false) CompletedTodoReqDto dto) {
        if (dto == null) dto = new CompletedTodoReqDto();
        return todoService.completedTodo(todoId, UUID.fromString(userId), dto);
    }

    // todo 미완료 처리
    @PutMapping("/incompletion/{todoId}")
    public TodoCreateResDto incompletedTodo(@RequestHeader("X-User-Id") String userId,
                                             @PathVariable String todoId,
                                            @RequestBody(required = false) IncompletedTodoReqDto dto) {
        if (dto == null) dto = new IncompletedTodoReqDto();
        return todoService.incompletedTodo(todoId, UUID.fromString(userId), dto);
    }
}
