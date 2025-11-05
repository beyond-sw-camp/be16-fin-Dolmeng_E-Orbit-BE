package com.Dolmeng_E.workspace.domain.task.repository;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    @Query("""
        select t
        from Task t
        where t.taskManager = :participant
          and t.isDone = false
          and t.endTime < :endTime
    """)
    List<Task> findUnfinishedTasksBeforeDate(
            @Param("participant") WorkspaceParticipant participant,
            @Param("endTime") LocalDateTime endTime
    );

    List<Task> findAllByStone(Stone stone);

    @Query("select t from Task t where t.stone.project.id = :projectId")
    List<Task> findAllByProjectId(@Param("projectId") String projectId);

    // 명시적 쿼리 (id 소문자)
    @Query("select t from Task t where t.taskManager.id = :taskManagerId")
    List<Task> findAllByTaskManagerId(@Param("taskManagerId") String taskManagerId);

    long countByStone(Stone stone);

    long countByStoneAndIsDoneTrue(Stone stone);

    // 프로젝트 내 전체 태스크 수 (삭제된 스톤 제외)
    @Query("""
        select count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
    """)
    long countTasksByProjectId(@Param("projectId") String projectId);

    // 프로젝트 내 완료 태스크 수
    @Query("""
        select count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
          and t.isDone = true
    """)
    long countDoneTasksByProjectId(@Param("projectId") String projectId);

    // ===== LLM 스냅샷용 =====

    // 프로젝트 전체 태스크 수(자식/손자 스톤 포함)
    @Query("""
        select count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
    """)
    long countAllByProjectId(@Param("projectId") String projectId);

    // 프로젝트 완료된 태스크 수
    @Query("""
        select count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
          and t.isDone = true
    """)
    long countCompletedByProjectId(@Param("projectId") String projectId);

    // 프로젝트 내 "지연 중(미완료 + 마감 초과)" 태스크 수
    @Query("""
        select count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
          and t.isDone = false
          and t.endTime < :now
    """)
    long countDelayedOpenByProjectId(@Param("projectId") String projectId,
                                     @Param("now") LocalDateTime now);

    // 프로젝트 내 재오픈 누적 합계 (reopenedCount 합)
    @Query("""
        select coalesce(sum(t.reopenedCount), 0)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
    """)
    long sumReopenedByProjectId(@Param("projectId") String projectId);

    // 최근 완료량 집계(윈도우 내 완료 태스크 수) - 예: 최근 7/30일
    @Query("""
        select count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
          and t.isDone = true
          and t.taskCompletedDate >= :from
          and t.taskCompletedDate <  :to
    """)
    long countCompletedBetween(@Param("projectId") String projectId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    // 평균 완료 시간(초) - start_time ~ task_completed_date, 완료된 태스크만 (네이티브)
    @Query(value = """
        select avg(TIMESTAMPDIFF(SECOND, t.start_time, t.task_completed_date))
        from task t
          join stone s on s.id = t.stone_id
        where s.project_id = :projectId
          and s.is_delete = false
          and t.is_done = true
          and t.task_completed_date is not null
    """, nativeQuery = true)
    Double avgCompletionSecondsByProjectId(@Param("projectId") String projectId);

    // 지연 태스크 조회 (정렬/상위 N개는 Pageable로 제어: PageRequest.of(0,3, Sort.by(ASC,"endTime")))
    @Query("""
        select t
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
          and t.isDone = false
          and t.endTime < :now
        order by t.endTime asc
    """)
    List<Task> findDelayedTasks(@Param("projectId") String projectId,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

    // 멤버별 태스크 수(Workload 분포 → workloadDeviation 계산용)
    @Query("""
        select t.taskManager.id, count(t)
        from Task t
        join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
        group by t.taskManager.id
    """)
    List<Object[]> countTasksGroupedByManager(@Param("projectId") String projectId);

    // 담당자별 전체/완료 태스크 수 집계
    @Query("""
        select t.taskManager.id,
               count(t),
               sum(case when t.isDone = true then 1 else 0 end)
        from Task t
          join t.stone s
        where s.project.id = :projectId
          and s.isDelete = false
        group by t.taskManager.id
    """)
    List<Object[]> countTotalsAndCompletedByManager(@Param("projectId") String projectId);

    // 프로젝트 기준으로 지연 중인 태스크 상위 3건 (오래 밀린 순)
    List<Task> findTop3ByStone_Project_IdAndIsDoneFalseAndEndTimeBeforeOrderByEndTimeAsc(
            String projectId, LocalDateTime now
    );

    // 완료된 태스크 전부 (완료일 최신순) — 엔티티 필드명에 맞게 정렬 기준 수정 (taskCompletedDate)
    @Query("""
        select t from Task t
          join t.stone s
         where s.project.id = :projectId
           and t.isDone = true
      order by t.taskCompletedDate desc
    """)
    List<Task> findAllByProjectIdAndIsDoneTrueOrderByCompletedAtDesc(@Param("projectId") String projectId);
}
