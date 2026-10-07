package com.projectsync.backend.repository;

import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findByProjectOrderByStartDateAsc(ProjectProposal project);
}
