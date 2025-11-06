package com.Dolmeng_E.workspace.domain.project.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectSnapshotDto {
    // 프로젝트의 메타/일정 정보 요약
    private ProjectDto project;
    // 태스크 진행 상태(수량/지표) 요약
    private ProgressDto progress;
    // 인원/분배 관련 요약
    private TeamDto team;
    // (선택) 캘린더/속도 변화 등 맥락 정보
    private ContextDto context;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    // 프로젝트 정보
    public static class ProjectDto {
        // 프로젝트명 (예: "HANSOOM v2.0")
        private String name;
        // 프로젝트 시작일시 (프로젝트 범위 계산, 진행률 해석의 기준)
        private LocalDateTime startDate;
        // 프로젝트 종료(마감)일시 (지연 판정, 잔여기간 계산의 기준)
        private LocalDateTime dueDate;
        // 스냅샷 생성 시각(서버 now) — "현재 시점" 기준값
        private LocalDateTime currentDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    // 진행률 정보
    public static class ProgressDto {
        // 전체 태스크 수 (삭제된 스톤 소속 제외)
        private int totalTasks;
        // 완료된 태스크 수 (isDone = true)
        private int completedTasks;
        // 남은 태스크 수 = totalTasks - completedTasks (하한 0)
        private int remainingTasks;
        // 평균 태스크 완료 소요 일수 (startTime → taskCompletedDate; 초 평균을 일로 변환)
        private double averageTaskCompletionTimeDays;
        // 최근 완료 추세(윈도우 카운트: 7/30일)
        private RecentCompletionRateDto recentCompletionRate;
        // 현재 지연 중인 태스크 수 (미완료 + endTime < now)
        private int delayedTasks;
        // 재오픈 누적 횟수(태스크 reopenedCount 합계)
        private int reopenedTasks;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class RecentCompletionRateDto {
            // 최근 7일 내 완료된 태스크 수
            private int last7Days;
            // 최근 30일 내 완료된 태스크 수
            private int last30Days;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    // 프로젝트 참여자 정보
    public static class TeamDto {
        // 프로젝트 참여자 수 (ProjectParticipant 기준)
        private int totalMembers;
        // 활성 멤버 수 (권장: 프로젝트 소속 + WorkspaceParticipant.isDelete = false)
        private int activeMembers;
        // 담당자별 완료율의 평균(%) — 각 담당자 (완료/전체) 비율의 산술평균을 %로 환산
        private int averageTasksPerMember;
        // 작업량 편차(CoV): 멤버별 태스크 수 표준편차 / 평균 태스크 수 (0에 가까울수록 균등)
        private double workloadDeviation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    // 넣을지 고민
    public static class ContextDto {
        // ✅ 주말 제외 계산 여부: 항상 true (빌더/기본 생성 모두에서 true 유지)
        @Builder.Default
        private boolean weekendsExcluded = true;
        // 휴일 목록(근무일 산정/예상 완료일 추정 시 반영)
        private List<LocalDate> holidays;
        // 속도 변화 설명 (예: "slightly increasing", "flat", "decreasing")
        private String velocityChange;
    }
}
