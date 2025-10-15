package com.Dolmeng_E.workspace.domain.stone.controller;

import com.Dolmeng_E.workspace.domain.stone.dto.*;
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
    @PatchMapping("/participant/join")
    public ResponseEntity<?> joinStoneParticipant(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody StoneParticipantListDto dto
    ) {
        stoneService.joinStoneParticipant(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 참여자 추가 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 스톤 참여자 리스트 삭제
    @DeleteMapping("/participant/list")
    public ResponseEntity<?> deleteStoneParticipantList(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody StoneParticipantListDto dto
    ) {
        stoneService.deleteStoneParticipantList(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 참여자 목록 삭제 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 스톤 참여자 일괄 삭제
    @DeleteMapping("/participant/all/{stoneId}")
    public ResponseEntity<?> deleteAllStoneParticipants(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String stoneId
    ) {
        stoneService.deleteAllStoneParticipants(userId, stoneId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 참여자 전체 삭제 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }

    // 스톤 프로젝트 캘린더에 노출 여부 설정(프로젝트 캘린더 조회용 API)
    @PatchMapping("/setting")
    public ResponseEntity<?> settingStone(@RequestHeader("X-User-Id") String userId,
                                         @RequestBody StoneSettingDto dto) {
        stoneService.settingStone(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 노출여부 설정 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 스톤 정보 수정(진행상태 변경 포함)
    @PatchMapping("")
    public ResponseEntity<?> modifyStone(@RequestHeader("X-User-Id") String userId,
                                         @RequestBody StoneModifyDto dto
    ) {
        stoneService.modifyStone(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 정보 수정 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 스톤 담당자 수정
    @PatchMapping("/manager")
    public ResponseEntity<?> modifyStoneManager(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody StoneManagerModifyDto dto
    ) {
        stoneService.modifyStoneManager(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 담당자 수정 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }


    // 내 마일스톤 목록 조회(삭제되지 않은 스톤 조회)

    // 스톤 삭제

    // 스톤 상세 정보 조회

    // 스톤 참여자 목록 조회

    // 태스크 생성(생성시 스톤의 task수 반영 필요)

    // 태스크 수정

    // 태스크 삭제(삭제시 스톤의 task수 반영 필요)

    // 태스크 완료 처리(완료시 스톤의 마일스톤 반영 필요)

    // 마일스톤 진행률 변경

    // To-Do: 다 하면 프로젝트 쪽 로직 완성

}
