package com.Dolmeng_E.workspace.domain.stone.controller;

import com.Dolmeng_E.workspace.common.dto.SubProjectResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.domain.stone.dto.*;
import com.Dolmeng_E.workspace.domain.stone.service.StoneService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 스톤 정보 수정
    @PatchMapping("")
    public ResponseEntity<?> modifyStone(@RequestHeader("X-User-Id") String userId,
                                         @RequestBody StoneModifyDto dto
    ) {
        String stoneId = stoneService.modifyStone(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 정보 수정 완료")
                .result(stoneId)
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

    // 스톤 삭제
    @DeleteMapping("/{stoneId}")
    public ResponseEntity<?> deleteStone(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String stoneId
    ) {
        stoneService.deleteStone(userId, stoneId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 삭제 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }

    // 스톤 완료처리
    @PatchMapping("/done/{stoneId}")
    public ResponseEntity<?> completeStone(@RequestHeader("X-User-Id") String userId,
                                         @PathVariable String stoneId
    ) {
        stoneService.completeStone(userId, stoneId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 완료처리 성공")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 프로젝트 별 내 마일스톤 조회(isDelete = true 제외, stoneStatus Completed 제외)
    @GetMapping("/milestone/{workspaceId}")
    public ResponseEntity<?> milestoneList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트별 내 마일스톤 조회 성공")
                .result(stoneService.milestoneList(userId, workspaceId))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 스톤 상세 정보 조회
    @GetMapping("/{stoneId}")
    public ResponseEntity<?> getStoneDetail(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String stoneId
    ) {

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 상세조회 성공")
                .result(stoneService.getStoneDetail(userId, stoneId))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 스톤 참여자 목록 조회
    @GetMapping("/participant/{stoneId}")
    public ResponseEntity<?> getStoneParticipantList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String stoneId
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 참여자 목록 조회 성공")
                .result(stoneService.getStoneParticipantList(userId, stoneId))
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }

    // 워크스페이스별 스톤 목록 조회
    @GetMapping("/list/{workspaceId}")
    public ResponseEntity<?> getStonesByWorkspace(
            @PathVariable String workspaceId,
            @RequestHeader("X-User-Id") String userId
    ) {
        List<StoneListResDto> stones = stoneService.getStonesByWorkspace(workspaceId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤 목록 조회 성공")
                .result(stones)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }
    // workspaceId 넘겼을 때 하위 프로젝트 Id, 프로젝트명 가져오는 api
    @GetMapping("/task/{stoneId}/sub-task")
    public List<SubTaskResDto> getSubTasks(@PathVariable String stoneId) {
        return stoneService.getSubTasksByStone(stoneId);
    }
}
