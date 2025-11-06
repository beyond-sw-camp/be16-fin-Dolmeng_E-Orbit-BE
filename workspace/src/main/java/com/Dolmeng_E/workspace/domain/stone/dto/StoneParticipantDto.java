package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StoneParticipantDto {
    private String participantId;
    private String participantName;
    private UUID userId;
    private String userEmail;
}
