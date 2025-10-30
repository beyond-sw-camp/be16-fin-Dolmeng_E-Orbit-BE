package com.Dolmeng_E.workspace.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceOrProjectManagerCheckDto {
    private boolean isWorkspaceManager;
    private boolean isProjectManager;
}
