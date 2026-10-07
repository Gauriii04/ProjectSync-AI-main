CREATE TABLE IF NOT EXISTS student_assessments (
    user_id          UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    frontend_score   INT NOT NULL CHECK (frontend_score BETWEEN 1 AND 10),
    backend_score    INT NOT NULL CHECK (backend_score BETWEEN 1 AND 10),
    database_score   INT NOT NULL CHECK (database_score BETWEEN 1 AND 10),
    devops_score     INT NOT NULL CHECK (devops_score BETWEEN 1 AND 10),
    ml_ai_score      INT NOT NULL CHECK (ml_ai_score BETWEEN 1 AND 10),
    primary_interest VARCHAR(50),
    past_projects    TEXT,
    updated_at       TIMESTAMPTZ NOT NULL
);
