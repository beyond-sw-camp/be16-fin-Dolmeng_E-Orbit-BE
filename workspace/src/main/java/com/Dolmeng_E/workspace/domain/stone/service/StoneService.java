package com.Dolmeng_E.workspace.domain.stone.service;

import com.Dolmeng_E.workspace.domain.stone.repository.ChildStoneListRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StoneService {
    private final ChildStoneListRepository childStoneListRepository;
    private final TaskRepository taskRepository;
    private final StoneRepository stoneRepository;


    // 스톤 생성

    // 스톤 수정

    // 스톤 삭제

    // 스톤 목록? 스톤들 뿌리처럼 보이는 거

    // 스톤 상세 정보 조회

    // 태스크 생성

    // 태스크 수정

    // 태스크 삭제

    // 태스크 완료 처리

    // 마일스톤 진행률 변경

    // To-Do: 다 하면 프로젝트 쪽 로직 완성

}
