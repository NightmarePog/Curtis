package com.sosehl.curtis.feature.sessions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SessionMediaAccessPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SessionMediaAccess access;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void studentAccessFollowsQuestionAndSessionVisibility() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID servedMediaId = UUID.randomUUID();
        UUID futureMediaId = UUID.randomUUID();

        insertUser(teacherId, "teacher");
        insertUser(studentId, "student");
        insertUser(otherStudentId, "other-student");
        jdbc.update(
            "INSERT INTO subjects (id, code, name) VALUES (?, ?, ?)",
            subjectId,
            "MEDIA",
            "Media access"
        );
        jdbc.update(
            "INSERT INTO classes (id, name, created_by) VALUES (?, ?, ?)",
            classId,
            "Media class",
            teacherId
        );
        jdbc.update(
            """
            INSERT INTO quizzes (
              id, creator_id, subject_id, title, status, max_questions_per_session
            ) VALUES (?, ?, ?, 'Media quiz', 'PUBLISHED', 2)
            """,
            quizId,
            teacherId,
            subjectId
        );
        jdbc.update(
            """
            INSERT INTO sessions (
              id, quiz_id, teacher_id, status, title_snapshot,
              teacher_name_snapshot, quiz_snapshot, started_at, closes_at
            ) VALUES (
              ?, ?, ?, 'ACTIVE', 'Media quiz', 'Teacher',
              jsonb_build_object(
                'questions', jsonb_build_array(
                  jsonb_build_object('mediaId', ?::text),
                  jsonb_build_object('mediaId', ?::text)
                )
              ),
              CURRENT_TIMESTAMP - INTERVAL '1 hour',
              CURRENT_TIMESTAMP + INTERVAL '1 hour'
            )
            """,
            sessionId,
            quizId,
            teacherId,
            servedMediaId,
            futureMediaId
        );
        jdbc.update(
            "INSERT INTO session_participants (session_id, student_id, class_id) VALUES (?, ?, ?)",
            sessionId,
            studentId,
            classId
        );
        jdbc.update(
            """
            INSERT INTO attempts (
              id, session_id, student_id, attempt_number, status, current_position
            ) VALUES (?, ?, ?, 1, 'IN_PROGRESS', 0)
            """,
            attemptId,
            sessionId,
            studentId
        );
        insertAttemptQuestion(attemptId, 0, servedMediaId, true);
        insertAttemptQuestion(attemptId, 1, futureMediaId, false);

        assertThat(access.canRead(studentId, servedMediaId)).isTrue();
        assertThat(access.canRead(studentId, futureMediaId)).isFalse();
        assertThat(access.canRead(otherStudentId, servedMediaId)).isFalse();
        assertThat(access.canRead(teacherId, futureMediaId)).isTrue();

        jdbc.update(
            "UPDATE sessions SET status = 'CLOSED', closed_at = CURRENT_TIMESTAMP WHERE id = ?",
            sessionId
        );
        assertThat(access.canRead(studentId, futureMediaId)).isTrue();

        jdbc.update(
            """
            UPDATE sessions
            SET status = 'ACTIVE', closed_at = NULL,
                closes_at = CURRENT_TIMESTAMP - INTERVAL '1 minute'
            WHERE id = ?
            """,
            sessionId
        );
        assertThat(access.canRead(studentId, futureMediaId)).isTrue();
    }

    private void insertUser(UUID id, String identity) {
        jdbc.update(
            """
            INSERT INTO users (id, issuer, subject, username, display_name)
            VALUES (?, 'test-issuer', ?, ?, ?)
            """,
            id,
            identity,
            identity + "@example.test",
            identity
        );
    }

    private void insertAttemptQuestion(
        UUID attemptId,
        int position,
        UUID mediaId,
        boolean served
    ) {
        jdbc.update(
            """
            INSERT INTO attempt_questions (
              id, attempt_id, position, source_question_id, question_snapshot,
              state, served_at, max_points, grading_state
            ) VALUES (
              ?, ?, ?, ?, jsonb_build_object('mediaId', ?::text),
              ?, CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END,
              1, 'AUTO_GRADED'
            )
            """,
            UUID.randomUUID(),
            attemptId,
            position,
            UUID.randomUUID(),
            mediaId,
            served ? "SERVED" : "READY",
            served
        );
    }
}
