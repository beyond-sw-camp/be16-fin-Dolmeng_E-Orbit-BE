package com.Dolmeng_E.workspace.domain.task.repository;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    @Query("""
        SELECT t
        FROM Task t
        WHERE t.taskManager = :participant
          AND t.isDone = false
          AND t.endTime < :endTime
    """)
    List<Task> findUnfinishedTasksBeforeDate(
            @Param("participant") WorkspaceParticipant participant,
            @Param("endTime") LocalDateTime endTime
    );
    List<Task> findAllByStone(Stone stone);

    @Query("SELECT t FROM Task t WHERE t.stone.project.id = :projectId")
    List<Task> findAllByProjectId(@Param("projectId") String projectId);

    // 기존 findAllByTaskManager_Id 대신 명시적 쿼리 작성
    @Query("SELECT t FROM Task t WHERE t.taskManager.Id = :taskManagerId")
    List<Task> findAllByTaskManagerId(@Param("taskManagerId") String taskManagerId);

    long countByStone(Stone stone);

    long countByStoneAndIsDoneTrue(Stone stone);
}
