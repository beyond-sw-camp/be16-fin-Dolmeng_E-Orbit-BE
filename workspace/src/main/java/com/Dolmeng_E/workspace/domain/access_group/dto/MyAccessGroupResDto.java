package com.Dolmeng_E.workspace.domain.access_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MyAccessGroupResDto {
    private boolean isProjectCreate;
    private boolean isStoneCreate;
    private boolean isProjectFileView;
    private boolean isStoneFileView;
    private boolean isWorkspaceFileView;
}
