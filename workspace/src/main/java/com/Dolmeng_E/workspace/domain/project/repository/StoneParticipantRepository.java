package com.Dolmeng_E.workspace.domain.project.repository;

import com.Dolmeng_E.workspace.domain.project.entity.StoneParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoneParticipantRepository extends JpaRepository<StoneParticipant, String> {
}
