package com.Dolmeng_E.workspace.domain.workspace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String inviteEmail;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private boolean isAccepted;

    @Column(nullable = false)
    private boolean isExistingUser;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean isPendingRegistration; // 비회원 초대용

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
