-- Baseline migration capturing the existing schema
-- Run with baseline-on-migrate=true to avoid conflicts with existing dev database

CREATE TABLE IF NOT EXISTS quizzes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    title VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP,
    edited_at TIMESTAMP,
    max_questions_per_session INTEGER,
    shuffle BOOLEAN,
    status VARCHAR(20),
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    CONSTRAINT uq_quizzes_uuid UNIQUE (uuid)
);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    question TEXT,
    type VARCHAR(30),
    points INTEGER,
    code_snippet TEXT,
    image_ref VARCHAR(255),
    time_in_seconds INTEGER,
    quiz_id BIGINT REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS question_answers (
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    answer TEXT,
    is_correct BOOLEAN
);

CREATE TABLE IF NOT EXISTS question_matching_pairs (
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    left_value TEXT,
    right_value TEXT
);

CREATE TABLE IF NOT EXISTS game_results (
    id BIGSERIAL PRIMARY KEY,
    session_uuid UUID,
    quiz_uuid UUID,
    student_id VARCHAR(255),
    score INTEGER,
    max_score INTEGER,
    played_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_results (
    id BIGSERIAL PRIMARY KEY,
    quiz_result_id BIGINT REFERENCES game_results(id) ON DELETE CASCADE,
    question_index INTEGER,
    question TEXT,
    type VARCHAR(30),
    points INTEGER,
    awarded_points INTEGER,
    status VARCHAR(30),
    text TEXT,
    code_snippet TEXT,
    image_ref VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS question_result_selected_indexes (
    question_result_id BIGINT NOT NULL REFERENCES question_results(id) ON DELETE CASCADE,
    selected_index INTEGER
);

CREATE TABLE IF NOT EXISTS question_result_matching_pairs (
    question_result_id BIGINT NOT NULL REFERENCES question_results(id) ON DELETE CASCADE,
    left_index INTEGER,
    right_index INTEGER
);

CREATE TABLE IF NOT EXISTS snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_hash VARCHAR(255),
    created_at TIMESTAMP,
    quiz_id BIGINT REFERENCES quizzes(id),
    quiz_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_quizzes_uuid ON quizzes(uuid);
CREATE INDEX IF NOT EXISTS idx_questions_quiz ON questions(quiz_id);
CREATE INDEX IF NOT EXISTS idx_game_results_session ON game_results(session_uuid);
CREATE INDEX IF NOT EXISTS idx_game_results_quiz ON game_results(quiz_uuid);
CREATE INDEX IF NOT EXISTS idx_game_results_student ON game_results(student_id);
CREATE INDEX IF NOT EXISTS idx_question_results_quiz_result ON question_results(quiz_result_id);
CREATE INDEX IF NOT EXISTS idx_snapshots_quiz ON snapshots(quiz_id);
