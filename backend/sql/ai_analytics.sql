-- Run once in the Supabase SQL editor, after student_assessments.sql.
-- "project" = an approved row in project_proposals.
-- sprints / project_tasks / code_commits are STUBS until member2-agile lands; drop them in favour of that branch's tables then.

CREATE TABLE IF NOT EXISTS sprints (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES project_proposals(id) ON DELETE CASCADE,
    name           VARCHAR(100) NOT NULL,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL CHECK (end_date >= start_date),
    planned_points INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_tasks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id        UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    assignee_user_id UUID REFERENCES users(id),
    title            VARCHAR(200) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'TODO',
    story_points     INT NOT NULL DEFAULT 0,
    completed_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS code_commits (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES project_proposals(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES users(id),
    sha            VARCHAR(64) NOT NULL,
    message        TEXT,
    lines_added    INT NOT NULL DEFAULT 0,
    lines_deleted  INT NOT NULL DEFAULT 0,
    committed_at   TIMESTAMPTZ NOT NULL,
    UNIQUE (project_id, sha)
);

CREATE TABLE IF NOT EXISTS student_contribution_metrics (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id              UUID NOT NULL REFERENCES project_proposals(id) ON DELETE CASCADE,
    sprint_id               UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    student_user_id         UUID NOT NULL REFERENCES users(id),
    total_commits           INT NOT NULL,
    lines_added             INT NOT NULL,
    lines_deleted           INT NOT NULL,
    tasks_completed         INT NOT NULL,
    consistency_score       DOUBLE PRECISION NOT NULL,
    contribution_percentage DOUBLE PRECISION NOT NULL,
    computed_at             TIMESTAMPTZ NOT NULL,
    UNIQUE (sprint_id, student_user_id)
);

CREATE TABLE IF NOT EXISTS ai_project_insights (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id        UUID NOT NULL REFERENCES project_proposals(id) ON DELETE CASCADE,
    sprint_id         UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    executive_summary TEXT,
    risk_level        VARCHAR(10) NOT NULL,
    anomaly_flags     TEXT NOT NULL,
    generated_at      TIMESTAMPTZ NOT NULL
);
