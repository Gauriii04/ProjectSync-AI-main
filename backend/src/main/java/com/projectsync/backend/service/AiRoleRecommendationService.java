package com.projectsync.backend.service;

import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.StudentAssessment;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectProposalRepository;
import com.projectsync.backend.repository.StudentAssessmentRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiRoleRecommendationService {

    public record RoleRecommendation(UUID studentUserId, String fullName, String role, String reason) {}

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectProposalRepository proposalRepository;
    private final StudentAssessmentRepository assessmentRepository;
    private final GeminiApiClient gemini;
    private final ObjectMapper objectMapper;

    public AiRoleRecommendationService(TeamRepository teamRepository,
                                       TeamMemberRepository teamMemberRepository,
                                       ProjectProposalRepository proposalRepository,
                                       StudentAssessmentRepository assessmentRepository,
                                       GeminiApiClient gemini,
                                       ObjectMapper objectMapper) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.proposalRepository = proposalRepository;
        this.assessmentRepository = assessmentRepository;
        this.gemini = gemini;
        this.objectMapper = objectMapper;
    }

    // ponytail: result is returned, not stored; add a column/table when roles must survive a page reload.
    public List<RoleRecommendation> recommendRoles(User currentUser, UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        teamMemberRepository.findByTeamAndStudentUser(team, currentUser)
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team."));

        ProjectProposal proposal = proposalRepository.findByTeam(team)
                .orElseThrow(() -> new BadRequestException("Create a project proposal before requesting role recommendations."));

        List<User> students = teamMemberRepository.findByTeam(team).stream()
                .map(TeamMember::getStudentUser)
                .toList();

        String profiles = students.stream()
                .map(s -> "- id=" + s.getId() + ", name=" + s.getFullName() + ", "
                        + assessmentRepository.findById(s.getId()).map(this::describe).orElse("no self-assessment submitted"))
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are assigning roles in a university software project team.
                Project title: %s
                Description: %s
                Tech stack: %s

                Team members (self-assessed skills, 1-10):
                %s

                Recommend one distinct role per member (e.g. Frontend Lead, Backend Architect,
                Database & DevOps, QA & Documentation Lead, AI/ML Engineer) and explain why in one or two sentences.
                Respond ONLY with a JSON array: [{"studentUserId": "<id>", "role": "<role>", "reason": "<reason>"}]
                """.formatted(proposal.getTitle(), proposal.getDescription(),
                Objects.requireNonNullElse(proposal.getTechnologyStack(), "not specified"), profiles);

        List<Map<String, String>> raw;
        try {
            raw = objectMapper.readValue(gemini.generateJson(prompt), new TypeReference<>() {});
        } catch (RuntimeException e) {
            throw new IllegalStateException("AI returned an unreadable recommendation.", e);
        }

        // Only keep entries for real team members; the model's output is untrusted.
        Map<String, User> byId = students.stream().collect(Collectors.toMap(s -> s.getId().toString(), s -> s));
        return raw.stream()
                .filter(r -> byId.containsKey(r.get("studentUserId")))
                .map(r -> {
                    User s = byId.get(r.get("studentUserId"));
                    return new RoleRecommendation(s.getId(), s.getFullName(), r.get("role"), r.get("reason"));
                })
                .toList();
    }

    private String describe(StudentAssessment a) {
        return "Frontend=%d, Backend=%d, Database=%d, DevOps=%d, ML/AI=%d, interest=%s, past projects=%s".formatted(
                a.getFrontendScore(), a.getBackendScore(), a.getDatabaseScore(), a.getDevopsScore(), a.getMlAiScore(),
                a.getPrimaryInterest(), a.getPastProjects());
    }
}
