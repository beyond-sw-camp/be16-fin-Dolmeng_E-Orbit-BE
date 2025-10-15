package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
public class StoneModifyDto {
    private String stoneId;
    private String stoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
//    private Boolean chatCreation; // 이미 채팅방이 생성되었는데 써야하는지 궁금
    private StoneStatus stoneStatus;
}
