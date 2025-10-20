package com.Dolmeng_E.workspace.domain.access_group.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessGroupResDto {
    private String accessGroupId;
    private String accessGroupName;

    // 권한 여부만 필드로 존재
    private boolean inviteUser;
    private boolean projectCreate;
    private boolean stoneCreate;
    private boolean userGroupCreate;
    private boolean projectFileView;
    private boolean stoneFileView;
    private boolean workspaceFileView;

}
