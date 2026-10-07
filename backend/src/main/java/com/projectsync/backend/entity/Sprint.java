package com.projectsync.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

// ponytail: stub until member2-agile's Sprint lands; "project" is the team's ProjectProposal.
@Entity
@Table(name = "sprints")
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectProposal project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "planned_points", nullable = false)
    private Integer plannedPoints = 0;

    public Sprint() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ProjectProposal getProject() { return project; }
    public void setProject(ProjectProposal project) { this.project = project; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getPlannedPoints() { return plannedPoints; }
    public void setPlannedPoints(Integer plannedPoints) { this.plannedPoints = plannedPoints; }
}
