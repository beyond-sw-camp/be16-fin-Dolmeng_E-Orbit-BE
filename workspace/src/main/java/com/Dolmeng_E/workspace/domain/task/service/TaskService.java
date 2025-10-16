package com.Dolmeng_E.workspace.domain.task.service;

import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    // 태스크 생성(생성시 스톤의 task수 반영 필요)

    // 태스크 수정

    // 태스크 삭제(삭제시 스톤의 task수 반영 필요)

    // 태스크 완료 처리(완료시 스톤의 마일스톤 반영 필요)
}
