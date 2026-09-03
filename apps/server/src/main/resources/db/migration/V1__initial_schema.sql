-- Curtis clean PostgreSQL baseline. Existing installations must start with an empty database.

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issuer VARCHAR(512) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    username VARCHAR(320) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    first_login_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_identity UNIQUE (issuer, subject),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('ADMINISTRATOR', 'TEACHER', 'STUDENT'))
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_subjects_code UNIQUE (code)
);

CREATE TABLE teacher_subjects (
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (teacher_id, subject_id)
);

CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_classes_name CHECK (length(trim(name)) > 0)
);

CREATE TABLE class_teachers (
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, teacher_id)
);

CREATE TABLE class_students (
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, student_id),
    CONSTRAINT uq_class_students_student UNIQUE (student_id)
);

CREATE TABLE class_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_class_groups_id_class UNIQUE (id, class_id)
);

CREATE TABLE group_students (
    group_id UUID NOT NULL,
    class_id UUID NOT NULL,
    student_id UUID NOT NULL,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, student_id),
    CONSTRAINT fk_group_students_group
        FOREIGN KEY (group_id, class_id) REFERENCES class_groups(id, class_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_students_roster
        FOREIGN KEY (class_id, student_id) REFERENCES class_students(class_id, student_id) ON DELETE CASCADE
);

CREATE TABLE media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    byte_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_media_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_media_byte_size CHECK (byte_size > 0)
);

CREATE TABLE quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE RESTRICT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(4000),
    chapter VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    max_questions_per_session INTEGER NOT NULL,
    shuffle BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_quizzes_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_quizzes_max_questions CHECK (max_questions_per_session > 0),
    CONSTRAINT ck_quizzes_validity CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to > valid_from)
);

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    type VARCHAR(32) NOT NULL,
    prompt VARCHAR(2000) NOT NULL,
    points INTEGER NOT NULL,
    code_snippet VARCHAR(20000),
    media_id UUID REFERENCES media(id) ON DELETE SET NULL,
    time_seconds INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_questions_position UNIQUE (quiz_id, position),
    CONSTRAINT ck_questions_position CHECK (position >= 0),
    CONSTRAINT ck_questions_points CHECK (points >= 0),
    CONSTRAINT ck_questions_time CHECK (time_seconds > 0)
);

CREATE TABLE question_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_question_options_position UNIQUE (question_id, position),
    CONSTRAINT ck_question_options_position CHECK (position >= 0)
);

CREATE TABLE matching_pairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    left_text VARCHAR(1000) NOT NULL,
    right_text VARCHAR(1000) NOT NULL,
    CONSTRAINT uq_matching_pairs_position UNIQUE (question_id, position),
    CONSTRAINT ck_matching_pairs_position CHECK (position >= 0)
);

CREATE TABLE yaml_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    source_path VARCHAR(1000) NOT NULL,
    content_digest VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    quiz_id UUID REFERENCES quizzes(id) ON DELETE SET NULL,
    error_details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_yaml_import_jobs_digest UNIQUE (content_digest),
    CONSTRAINT ck_yaml_import_jobs_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE RESTRICT,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status VARCHAR(24) NOT NULL,
    title_snapshot VARCHAR(100) NOT NULL,
    description_snapshot VARCHAR(4000),
    subject_snapshot VARCHAR(100),
    chapter_snapshot VARCHAR(100),
    teacher_name_snapshot VARCHAR(255) NOT NULL,
    quiz_snapshot JSONB NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closes_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    max_attempts INTEGER NOT NULL DEFAULT 1,
    score_policy VARCHAR(16) NOT NULL DEFAULT 'BEST',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_sessions_status CHECK (status IN ('ACTIVE', 'CLOSED', 'EXPIRED')),
    CONSTRAINT ck_sessions_attempts CHECK (max_attempts BETWEEN 1 AND 10),
    CONSTRAINT ck_sessions_score_policy CHECK (score_policy IN ('BEST', 'LATEST', 'ALL')),
    CONSTRAINT ck_sessions_close_time CHECK (closes_at > started_at)
);

CREATE TABLE session_classes (
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    class_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (session_id, class_id)
);

CREATE TABLE session_groups (
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    group_id UUID NOT NULL,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    group_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (session_id, group_id),
    CONSTRAINT uq_session_groups_target UNIQUE (session_id, group_id, class_id),
    CONSTRAINT fk_session_groups_group FOREIGN KEY (group_id, class_id)
        REFERENCES class_groups(id, class_id) ON DELETE RESTRICT
);

CREATE TABLE session_participants (
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    PRIMARY KEY (session_id, student_id),
    CONSTRAINT uq_session_participants_class UNIQUE (session_id, student_id, class_id)
);

CREATE TABLE session_participant_groups (
    session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    group_id UUID NOT NULL,
    class_id UUID NOT NULL,
    PRIMARY KEY (session_id, student_id, group_id),
    CONSTRAINT fk_participant_groups_participant
        FOREIGN KEY (session_id, student_id, class_id)
        REFERENCES session_participants(session_id, student_id, class_id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_groups_target
        FOREIGN KEY (session_id, group_id, class_id)
        REFERENCES session_groups(session_id, group_id, class_id) ON DELETE CASCADE
);

CREATE TABLE attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    current_position INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    graded_at TIMESTAMPTZ,
    score INTEGER NOT NULL DEFAULT 0,
    max_score INTEGER NOT NULL DEFAULT 0,
    pending_review_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attempts_number UNIQUE (session_id, student_id, attempt_number),
    CONSTRAINT fk_attempts_participant FOREIGN KEY (session_id, student_id)
        REFERENCES session_participants(session_id, student_id) ON DELETE CASCADE,
    CONSTRAINT ck_attempts_status CHECK (
        status IN ('IN_PROGRESS', 'SUBMITTED', 'PENDING_REVIEW', 'GRADED', 'EXPIRED')
    ),
    CONSTRAINT ck_attempts_number CHECK (attempt_number BETWEEN 1 AND 10),
    CONSTRAINT ck_attempts_position CHECK (current_position >= 0),
    CONSTRAINT ck_attempts_score CHECK (score >= 0 AND max_score >= 0),
    CONSTRAINT ck_attempts_pending CHECK (pending_review_count >= 0)
);

CREATE TABLE attempt_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES attempts(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    source_question_id UUID NOT NULL,
    question_snapshot JSONB NOT NULL,
    response JSONB,
    state VARCHAR(24) NOT NULL,
    served_at TIMESTAMPTZ,
    deadline_at TIMESTAMPTZ,
    answered_at TIMESTAMPTZ,
    max_points INTEGER NOT NULL,
    awarded_points INTEGER,
    grading_state VARCHAR(24) NOT NULL,
    graded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    graded_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_attempt_questions_position UNIQUE (attempt_id, position),
    CONSTRAINT ck_attempt_questions_state CHECK (
        state IN ('READY', 'SERVED', 'ANSWERED', 'TIMED_OUT')
    ),
    CONSTRAINT ck_attempt_questions_grading CHECK (
        grading_state IN ('AUTO_GRADED', 'PENDING_REVIEW', 'MANUALLY_GRADED')
    ),
    CONSTRAINT ck_attempt_questions_position CHECK (position >= 0),
    CONSTRAINT ck_attempt_questions_points CHECK (
        max_points >= 0 AND (awarded_points IS NULL OR (awarded_points >= 0 AND awarded_points <= max_points))
    )
);

CREATE INDEX idx_users_display_name ON users (display_name);
CREATE UNIQUE INDEX uq_subjects_name_ci ON subjects (lower(name));
CREATE UNIQUE INDEX uq_classes_name_ci ON classes (lower(name));
CREATE UNIQUE INDEX uq_class_groups_name_ci ON class_groups (class_id, lower(name));
CREATE INDEX idx_user_roles_role ON user_roles (role, user_id);
CREATE INDEX idx_teacher_subjects_subject ON teacher_subjects (subject_id, teacher_id);
CREATE INDEX idx_class_teachers_teacher ON class_teachers (teacher_id, class_id);
CREATE INDEX idx_class_groups_class ON class_groups (class_id, active);
CREATE INDEX idx_group_students_student ON group_students (student_id, group_id);
CREATE INDEX idx_quizzes_creator ON quizzes (creator_id, status, updated_at DESC);
CREATE INDEX idx_quizzes_subject ON quizzes (subject_id);
CREATE INDEX idx_sessions_teacher ON sessions (teacher_id, started_at DESC);
CREATE INDEX idx_sessions_status_close ON sessions (status, closes_at);
CREATE INDEX idx_session_participants_student ON session_participants (student_id, session_id);
CREATE INDEX idx_attempts_student ON attempts (student_id, started_at DESC);
CREATE INDEX idx_attempts_session_status ON attempts (session_id, status);
-- Spring Session JDBC schema, owned by Flyway rather than runtime initialization.
CREATE TABLE spring_session (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INTEGER NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(320),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session(primary_id) ON DELETE CASCADE
);
