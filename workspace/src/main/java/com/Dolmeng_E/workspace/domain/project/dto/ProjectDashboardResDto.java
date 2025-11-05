package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.common.dto.ElementCountAndSizeResDto;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProjectDashboardResDto {
    private BigDecimal projectMilestone;
    private Integer totalStoneCount;
    private Integer completedStoneCount;
    private Integer totalTaskCount;
    private Integer completedTaskCount;

    // 평균 태스크 완료시간
    private Integer avgTaskCompletedTime;

    // 태스크 리스크 지수(밀린태스크 수 / 전체 태스크 수)
    private BigDecimal taskRisk;

    // 지연되고 있는 태스크
    private List<LazyTask> lazyTasklist;

    // 완료된 스톤들 정보
    private List<CompletedStone> completedStoneList;

    // 완료된 태스크들 정보
    private List<CompletedTask> completedTaskList;

    // 예상 완료일 ?
    private LocalDateTime predictCompleteDay;

    // 문서 수 + 용량
    private ElementCountAndSizeResDto elementCountAndSizeResDto;




    public static ProjectDashboardResDto fromEntity(Project project,
                                            int totalTaskCount, int completedTaskCount,
                                            int totalStoneCount, int completedStoneCount,
                                            BigDecimal projectProgress,
                                            Integer avgTaskCompletedTime,
                                            BigDecimal taskRisk,
                                            List<LazyTask> lazyTasklist,
                                            List<CompletedStone> completedStoneList,
                                            List<CompletedTask> completedTaskList,
                                            LocalDateTime predictCompleteDay,
                                            ElementCountAndSizeResDto elementCountAndSizeResDto) {
        return ProjectDashboardResDto.builder()
                .projectMilestone(project != null ? project.getMilestone() : null)
                .totalStoneCount(totalStoneCount)
                .completedStoneCount(completedStoneCount)
                .totalTaskCount(totalTaskCount)
                .completedTaskCount(completedTaskCount)
//                .projectProgress(projectProgress)
                .avgTaskCompletedTime(avgTaskCompletedTime)
                .taskRisk(taskRisk)
                .lazyTasklist(lazyTasklist)
                .completedStoneList(completedStoneList)
                .completedTaskList(completedTaskList)
                .predictCompleteDay(predictCompleteDay)
                .elementCountAndSizeResDto(elementCountAndSizeResDto)
                .build();
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class LazyTask{
        private String taskName;
        private Integer lazyDay;
        private String taskManagerName;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class CompletedStone{
        private String stoneId;
        private String stoneName;
        private LocalDateTime stoneCompletedDay;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class CompletedTask{
        private String taskId;
        private String taskName;
        private LocalDateTime taskCompletedDay;
    }


}
