package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateTodoReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import com.Dolmeng_E.user.domain.sharedCalendar.repository.SharedCalendarRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TodoService {

    private final SharedCalendarRepository sharedCalendarRepository;
    private final CalendarValidationService validationService;

    // todo 등록
    public TodoCreateResDto createTodo(UUID userId, TodoCreateReqDto dto) {

        // 1. 검증
        var user = validationService.validateUserAndWorkspace(userId, dto.getWorkspaceId());

        // 2. 날짜 변환: 하루 전체로 설정
        LocalDateTime startedAt = dto.getDate().atStartOfDay();              // 2025-10-20T00:00:00
        LocalDateTime endedAt = dto.getDate().atTime(23, 59, 59);            // 2025-10-20T23:59:59

        // 3. todo 생성
        SharedCalendar todo = SharedCalendar.builder()
                .userId(user)
                .workspaceId(dto.getWorkspaceId())
                .calendarType(dto.getCalendarType())
                .calendarName(dto.getCalendarName())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .bookmark(dto.getBookmark())
                .build();

        sharedCalendarRepository.save(todo);
        return TodoCreateResDto.fromEntity(todo);
    }

    // todo 리스트 조회
    public List<TodoCreateResDto> getTodo(UUID userId, String workspaceId) {
        // 1. 검증
        validationService.validateUserAndWorkspace(userId, workspaceId);

        // 2. TODO만 조회
        List<SharedCalendar> todo = sharedCalendarRepository
                .findTodosByUserIdAndWorkspaceIdAndCalendarType(userId, workspaceId, CalendarType.TODO);

        return todo.stream()
                .map(TodoCreateResDto::fromEntity)
                .toList();
    }


    // todo 수정
    public TodoCreateResDto updateTodo(String todoId, UUID userId, UpdateTodoReqDto dto) {
        // 1. 검증
        var todo = sharedCalendarRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 todo입니다."));

        validationService.validateUserAndWorkspace(userId, todo.getWorkspaceId());

        if (!todo.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 todo만 수정할 수 있습니다.");

        if (todo.getCalendarType() != CalendarType.TODO) {
            throw new IllegalArgumentException("calendarType이 TODO인 일정만 수정할 수 있습니다.");
        }

        // 2. todo 수정
        todo.updateTodo(dto.getCalendarName(), dto.getDate(), dto.getBookmark());
        return TodoCreateResDto.fromEntity(todo);
    }

    // todo 삭제
    public void deleteTodo(String todoId, UUID userId) {
        // 1. 검증
        var todo = sharedCalendarRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않은 todo입니다."));

        validationService.validateUserAndWorkspace(userId, todo.getWorkspaceId());

        if (!todo.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 todo만 삭제할 수 있습니다.");

        // 2. todo 삭제
        sharedCalendarRepository.delete(todo);
    }


    // todo 완료 처리
}
