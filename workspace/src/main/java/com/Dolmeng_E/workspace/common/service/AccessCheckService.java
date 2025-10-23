package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessDetailRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessCheckService {

    private final AccessGroupRepository accessGroupRepository;
    private final AccessDetailRepository accessDetailRepository;

    // 공통 서비스 로직: 권한 체크
    public void validateAccess(WorkspaceParticipant participant, String accessListId) {
        String accessGroupId = participant.getAccessGroup().getId();
        AccessGroup accessGroup = accessGroupRepository.findById(accessGroupId)
                .orElseThrow(() -> new EntityNotFoundException("권한그룹이 존재하지 않습니다."));

        AccessDetail accessDetail = accessDetailRepository
                .findByAccessGroupAndAccessListId(accessGroup, accessListId)
                .orElseThrow(() -> new EntityNotFoundException("권한 상세정보가 없습니다."));

        if (!accessDetail.getIsAccess()) {
            throw new IllegalArgumentException("해당 작업에 대한 권한이 없습니다.");
        }
    }

    /*
    <권한 리스트>

    PROJECT_CREATE,      // ws_acc_list_1. 프로젝트 생성
    STONE_CREATE,        // ws_acc_list_2. 스톤 생성
    PROJECT_FILE_VIEW,   // ws_acc_list_3. 프로젝트별 파일 조회
    STONE_FILE_VIEW,     // ws_acc_list_4. 스톤별 파일 조회
    WORKSPACE_FILE_VIEW  // ws_acc_list_5. 워크스페이스별 파일 조회
    */
}
