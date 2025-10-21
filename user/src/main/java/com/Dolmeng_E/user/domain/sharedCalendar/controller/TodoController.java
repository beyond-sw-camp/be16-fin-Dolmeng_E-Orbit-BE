package com.Dolmeng_E.user.domain.sharedCalendar.controller;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateTodoReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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

    // todo 리스트 조회
    @GetMapping("/{workspaceId}")
    public List<TodoCreateResDto> getTodo(@RequestHeader("X-User-Id") String userId,
                                          @PathVariable String workspaceId) {
        return todoService.getTodo(UUID.fromString(userId), workspaceId);
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
}
