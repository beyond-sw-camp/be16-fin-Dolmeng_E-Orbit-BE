package com.Dolmeng_E.workspace.domain.stone.controller;

import com.Dolmeng_E.workspace.domain.stone.dto.StoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.dto.TopStoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.service.StoneService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stone")
@RequiredArgsConstructor
public class StoneController {
    private final StoneService stoneService;

    // 최상위 스톤 생성(프로젝트 생성 시 자동 생성)
    @PostMapping("/top")
    public ResponseEntity<?> createTopStone(
            @RequestBody TopStoneCreateDto dto
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("최상위 스톤 생성 완료")
                .result(stoneService.createTopStone(dto))
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

    // 일반 스톤 생성
    @PostMapping("")
    public ResponseEntity<?> createStone(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody StoneCreateDto dto
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 생성 완료")
                .result(stoneService.createStone(userId, dto))
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

    // 스톤 참여자 추가

    // 스톤 안보임 설정(프로젝트 캘린더 조회용 API)

    // 내 스톤 목록 조회

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
