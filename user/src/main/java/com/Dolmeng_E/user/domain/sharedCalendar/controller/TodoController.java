package com.Dolmeng_E.user.domain.sharedCalendar.controller;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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


    // todo 수정


    // todo 삭제
}
