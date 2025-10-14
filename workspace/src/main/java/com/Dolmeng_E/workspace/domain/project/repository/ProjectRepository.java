package com.Dolmeng_E.workspace.domain.project.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findAllByWorkspaceIdAndIsDeleteFalseAndProjectStatusNot(String workspaceId, ProjectStatus projectStatus);
}
