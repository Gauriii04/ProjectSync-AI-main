package com.projectsync.backend.service;

import com.projectsync.backend.entity.CodeCommit;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.service.AiCodeInspectorService.RiskFlag;
import com.projectsync.backend.service.ContributionAnalyticsService.StudentStats;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ContributionMathTest {

    static final OffsetDateTime START = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void volumeCountsDeletionsAtHalf() {
        assertEquals(150.0, ContributionAnalyticsService.volume(100, 100));
    }

    @Test
    void consistencySteadyVsSpike() {
        List<OffsetDateTime> daily = IntStream.range(0, 7).mapToObj(d -> START.plusDays(d).plusHours(12)).toList();
        assertEquals(1.0, ContributionAnalyticsService.consistency(daily, START, 7), 1e-9);

        List<OffsetDateTime> spike = IntStream.range(0, 7).mapToObj(i -> START.plusDays(6)).toList();
        assertEquals(0.0, ContributionAnalyticsService.consistency(spike, START, 7), 1e-9);

        assertEquals(0.0, ContributionAnalyticsService.consistency(List.of(), START, 7));
    }

    @Test
    void sharesAndRulesFireOnFreeloaderTeam() {
        User coder = user("Coder"), rider = user("Rider"), dumper = user("Dumper");

        List<CodeCommit> commits = new ArrayList<>();
        for (int d = 0; d < 7; d++) commits.add(commit(coder, "c" + d, 100, 0, START.plusDays(d)));
        commits.add(commit(dumper, "dump", 6000, 0, START.plusDays(6)));

        List<ProjectTask> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) tasks.add(doneTask(rider));

        List<StudentStats> stats = ContributionAnalyticsService.compute(List.of(coder, rider, dumper), commits, tasks, START, 7);
        assertEquals(700.0 / 6700, stats.get(0).share(), 1e-9);
        assertEquals(5, stats.get(1).tasksCompleted());

        List<RiskFlag> flags = AiCodeInspectorService.detectAnomalies(stats, commits, commits);
        List<String> rules = flags.stream().map(RiskFlag::rule).toList();
        assertTrue(rules.contains("FREE_RIDER"), rules.toString());
        assertTrue(rules.contains("MASSIVE_CODE_DUMP"), rules.toString());
        assertTrue(rules.contains("IMBALANCE"), rules.toString()); // dumper has ~90%
        assertEquals("HIGH", AiCodeInspectorService.riskLevel(flags));

        assertEquals("Incomplete", EvaluationExportService.suggestedGrade(stats.get(1), 3));
        assertEquals("A", EvaluationExportService.suggestedGrade(stats.get(2), 3));
        assertEquals("C", EvaluationExportService.suggestedGrade(stats.get(0), 3));
    }

    @Test
    void healthyTeamIsLowRisk() {
        User a = user("A"), b = user("B");
        List<CodeCommit> commits = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            commits.add(commit(a, "a" + d, 50, 10, START.plusDays(d)));
            commits.add(commit(b, "b" + d, 50, 10, START.plusDays(d)));
        }
        List<StudentStats> stats = ContributionAnalyticsService.compute(List.of(a, b), commits, List.of(doneTask(a), doneTask(b)), START, 7);
        List<RiskFlag> flags = AiCodeInspectorService.detectAnomalies(stats, commits, commits);
        assertEquals(List.of(), flags);
        assertEquals("LOW", AiCodeInspectorService.riskLevel(flags));
    }

    static User user(String name) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFullName(name);
        return u;
    }

    static CodeCommit commit(User author, String sha, int added, int deleted, OffsetDateTime at) {
        CodeCommit c = new CodeCommit();
        c.setAuthor(author);
        c.setSha(sha);
        c.setLinesAdded(added);
        c.setLinesDeleted(deleted);
        c.setCommittedAt(at);
        return c;
    }

    static ProjectTask doneTask(User assignee) {
        ProjectTask t = new ProjectTask();
        t.setAssignee(assignee);
        t.setStatus("DONE");
        t.setStoryPoints(3);
        return t;
    }
}
