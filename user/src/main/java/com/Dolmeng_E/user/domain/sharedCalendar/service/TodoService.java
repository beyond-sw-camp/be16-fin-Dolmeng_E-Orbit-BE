package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.TodoCreateResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import com.Dolmeng_E.user.domain.sharedCalendar.repository.SharedCalendarRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

        // 2. todo 생성
        SharedCalendar todo = SharedCalendar.builder()
                .userId(user)
                .workspaceId(dto.getWorkspaceId())
                .calendarType(dto.getCalendarType())
                .calendarName(dto.getCalendarName())
                .startedAt(dto.getStartedAt())
                .endedAt(dto.getEndedAt())
                .bookmark(dto.getBookmark())
                .build();

        sharedCalendarRepository.save(todo);
        return TodoCreateResDto.fromEntity(todo);
    }

    // todo 리스트 조회


    // todo 수정


    // todo 삭제
}
