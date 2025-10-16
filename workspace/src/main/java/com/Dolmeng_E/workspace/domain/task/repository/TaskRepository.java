package com.Dolmeng_E.workspace.domain.task.repository;

import com.Dolmeng_E.workspace.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
}
