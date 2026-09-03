package com.sosehl.curtis.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sosehl.curtis.feature.classrooms.ClassroomAccessService;
import com.sosehl.curtis.feature.classrooms.core.ResolvedAudience;
import com.sosehl.curtis.feature.sessions.ranking.RankingService;
import com.sosehl.curtis.feature.sessions.students.TeacherStudentQueryService;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class InitialSchemaMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );

    @Autowired
    private ClassroomAccessService classroomAccess;

    @Autowired
    private TeacherStudentQueryService teacherStudents;

    @Autowired
    private RankingService rankings;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void baselineCreatesEveryFoundationAndRuntimeTable() throws SQLException {
        List<String> expected = List.of(
            "users",
            "user_roles",
            "subjects",
            "teacher_subjects",
            "classes",
            "class_teachers",
            "class_students",
            "class_groups",
            "group_students",
            "quizzes",
            "sessions",
            "attempts",
            "attempt_questions",
            "spring_session",
            "spring_session_attributes"
        );
        try (
            Connection connection = POSTGRES.createConnection("");
            PreparedStatement statement = connection.prepareStatement(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name=?)"
            )
        ) {
            for (String table : expected) {
                statement.setString(1, table);
                try (ResultSet result = statement.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getBoolean(1)).as(table).isTrue();
                }
            }
        }
    }

    @Test
    void databaseEnforcesOneClassPerStudentAndGroupRosterIntegrity()
        throws SQLException {
        UUID adminId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID firstClassId = UUID.randomUUID();
        UUID secondClassId = UUID.randomUUID();
        UUID secondClassGroupId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            insertUser(connection, adminId, "admin", "admin@school.cz");
            insertUser(connection, studentId, "student", "student@school.cz");
            execute(
                connection,
                "INSERT INTO classes (id,name,created_by) VALUES (?,?,?)",
                firstClassId,
                "1.A",
                adminId
            );
            execute(
                connection,
                "INSERT INTO classes (id,name,created_by) VALUES (?,?,?)",
                secondClassId,
                "1.B",
                adminId
            );
            execute(
                connection,
                "INSERT INTO class_students (class_id,student_id,assigned_by) VALUES (?,?,?)",
                firstClassId,
                studentId,
                adminId
            );

            assertThatThrownBy(() ->
                execute(
                    connection,
                    "INSERT INTO class_students (class_id,student_id,assigned_by) VALUES (?,?,?)",
                    secondClassId,
                    studentId,
                    adminId
                )
            ).isInstanceOf(SQLException.class);

            execute(
                connection,
                "INSERT INTO class_groups (id,class_id,name,created_by) VALUES (?,?,?,?)",
                secondClassGroupId,
                secondClassId,
                "Group B",
                adminId
            );
            assertThatThrownBy(() ->
                execute(
                    connection,
                    "INSERT INTO group_students (group_id,class_id,student_id,assigned_by) VALUES (?,?,?,?)",
                    secondClassGroupId,
                    secondClassId,
                    studentId,
                    adminId
                )
            ).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void audienceResolutionSupportsSharedClassesAndNestedGroups()
        throws SQLException {
        UUID adminId = UUID.randomUUID();
        UUID firstTeacherId = UUID.randomUUID();
        UUID secondTeacherId = UUID.randomUUID();
        UUID firstStudentId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            insertUser(connection, adminId, "admin-2", uniqueUsername("admin"));
            insertUser(connection, firstTeacherId, "teacher-1", uniqueUsername("teacher"));
            insertUser(connection, secondTeacherId, "teacher-2", uniqueUsername("teacher"));
            insertUser(connection, firstStudentId, "student-1", uniqueUsername("student"));
            insertUser(connection, secondStudentId, "student-2", uniqueUsername("student"));
            execute(
                connection,
                "INSERT INTO user_roles (user_id,role) VALUES (?,'TEACHER'),(?,'TEACHER'),(?,'STUDENT'),(?,'STUDENT')",
                firstTeacherId,
                secondTeacherId,
                firstStudentId,
                secondStudentId
            );
            execute(
                connection,
                "INSERT INTO classes (id,name,created_by) VALUES (?,?,?)",
                classId,
                "Shared " + classId,
                adminId
            );
            execute(
                connection,
                "INSERT INTO class_teachers (class_id,teacher_id,assigned_by) VALUES (?,?,?),(?,?,?)",
                classId,
                firstTeacherId,
                adminId,
                classId,
                secondTeacherId,
                adminId
            );
            execute(
                connection,
                "INSERT INTO class_students (class_id,student_id,assigned_by) VALUES (?,?,?),(?,?,?)",
                classId,
                firstStudentId,
                adminId,
                classId,
                secondStudentId,
                adminId
            );
            execute(
                connection,
                "INSERT INTO class_groups (id,class_id,name,created_by) VALUES (?,?,?,?)",
                groupId,
                classId,
                "Selected group",
                firstTeacherId
            );
            execute(
                connection,
                "INSERT INTO group_students (group_id,class_id,student_id,assigned_by) VALUES (?,?,?,?)",
                groupId,
                classId,
                firstStudentId,
                firstTeacherId
            );
        }

        ResolvedAudience groupAudience = classroomAccess.resolveAudience(
            secondTeacherId,
            Set.of(),
            Set.of(groupId)
        );
        assertThat(groupAudience.groups()).hasSize(1);
        assertThat(groupAudience.students()).hasSize(1);
        assertThat(groupAudience.students().get(0).targetedGroupIds())
            .containsExactly(groupId);

        ResolvedAudience classAudience = classroomAccess.resolveAudience(
            firstTeacherId,
            Set.of(classId),
            Set.of()
        );
        assertThat(classAudience.students()).hasSize(2);

        assertThatThrownBy(() ->
            classroomAccess.resolveAudience(
                firstTeacherId,
                Set.of(classId),
                Set.of(groupId)
            )
        )
            .isInstanceOf(ProblemException.class)
            .satisfies(exception ->
                assertThat(((ProblemException) exception).code())
                    .isEqualTo("redundant_audience_target")
            );
    }

    @Test
    void historicalTeacherCannotSeeStudentsCurrentUnassignedClass()
        throws SQLException {
        UUID administratorId = UUID.randomUUID();
        UUID historicalTeacherId = UUID.randomUUID();
        UUID currentTeacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID currentClassId = UUID.randomUUID();
        UUID currentGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        try (Connection connection = POSTGRES.createConnection("")) {
            insertUser(connection, administratorId, "privacy-admin", uniqueUsername("admin"));
            insertUser(connection, historicalTeacherId, "historical-teacher", uniqueUsername("teacher"));
            insertUser(connection, currentTeacherId, "current-teacher", uniqueUsername("teacher"));
            insertUser(connection, studentId, "moved-student", uniqueUsername("student"));
            execute(
                connection,
                "INSERT INTO classes (id,name,created_by) VALUES (?,?,?)",
                currentClassId,
                "Private current class " + currentClassId,
                administratorId
            );
            execute(
                connection,
                "INSERT INTO class_teachers (class_id,teacher_id,assigned_by) VALUES (?,?,?)",
                currentClassId,
                currentTeacherId,
                administratorId
            );
            execute(
                connection,
                "INSERT INTO class_students (class_id,student_id,assigned_by) VALUES (?,?,?)",
                currentClassId,
                studentId,
                administratorId
            );
            execute(
                connection,
                "INSERT INTO class_groups (id,class_id,name,created_by) VALUES (?,?,?,?)",
                currentGroupId,
                currentClassId,
                "Private current group",
                currentTeacherId
            );
            execute(
                connection,
                "INSERT INTO group_students (group_id,class_id,student_id,assigned_by) VALUES (?,?,?,?)",
                currentGroupId,
                currentClassId,
                studentId,
                currentTeacherId
            );
            execute(
                connection,
                "INSERT INTO subjects (id,code,name) VALUES (?,?,?)",
                subjectId,
                "PRIV_" + subjectId.toString().substring(0, 8),
                "Privacy subject " + subjectId
            );
            execute(
                connection,
                """
                INSERT INTO quizzes (
                    id, creator_id, subject_id, title, status,
                    max_questions_per_session, shuffle
                ) VALUES (?,?,?,?,'PUBLISHED',1,FALSE)
                """,
                quizId,
                historicalTeacherId,
                subjectId,
                "Historical quiz"
            );
            execute(
                connection,
                """
                INSERT INTO sessions (
                    id, quiz_id, teacher_id, status, title_snapshot,
                    teacher_name_snapshot, quiz_snapshot, started_at, closes_at
                ) VALUES (?,?,?,'CLOSED',?,?,'{\"questions\":[]}'::jsonb,
                          CURRENT_TIMESTAMP - INTERVAL '2 hours',
                          CURRENT_TIMESTAMP - INTERVAL '1 hour')
                """,
                sessionId,
                quizId,
                historicalTeacherId,
                "Historical quiz",
                "Historical teacher"
            );
            execute(
                connection,
                "INSERT INTO session_participants (session_id,student_id,class_id) VALUES (?,?,?)",
                sessionId,
                studentId,
                currentClassId
            );
            execute(
                connection,
                """
                INSERT INTO attempts (
                    id, session_id, student_id, attempt_number, status,
                    started_at, submitted_at, score, max_score
                ) VALUES (?,?,?,1,'GRADED',CURRENT_TIMESTAMP - INTERVAL '2 hours',
                          CURRENT_TIMESTAMP - INTERVAL '1 hour',1,1)
                """,
                attemptId,
                sessionId,
                studentId
            );
        }

        var profile = teacherStudents.profile(
            historicalTeacherId,
            studentId
        );
        assertThat(profile.classId()).isNull();
        assertThat(profile.className()).isNull();
        assertThat(profile.groups()).isEmpty();
        assertThat(profile.attemptCount()).isEqualTo(1);
    }

    @Test
    void leaderboardUsesLaunchTimeParticipantTargetAfterRosterMove()
        throws SQLException {
        UUID administratorId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID launchClassId = UUID.randomUUID();
        UUID currentClassId = UUID.randomUUID();
        UUID launchGroupId = UUID.randomUUID();
        UUID currentGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        try (Connection connection = POSTGRES.createConnection("")) {
            insertUser(connection, administratorId, "ranking-admin", uniqueUsername("admin"));
            insertUser(connection, teacherId, "ranking-teacher", uniqueUsername("teacher"));
            insertUser(connection, studentId, "ranking-student", uniqueUsername("student"));
            execute(
                connection,
                "INSERT INTO user_roles (user_id,role) VALUES (?,'STUDENT')",
                studentId
            );
            execute(
                connection,
                "INSERT INTO classes (id,name,created_by) VALUES (?,?,?),(?,?,?)",
                launchClassId,
                "Launch class " + launchClassId,
                administratorId,
                currentClassId,
                "Current class " + currentClassId,
                administratorId
            );
            execute(
                connection,
                "INSERT INTO class_students (class_id,student_id,assigned_by) VALUES (?,?,?)",
                currentClassId,
                studentId,
                administratorId
            );
            execute(
                connection,
                "INSERT INTO class_groups (id,class_id,name,created_by) VALUES (?,?,?,?),(?,?,?,?)",
                launchGroupId,
                launchClassId,
                "Launch group",
                teacherId,
                currentGroupId,
                currentClassId,
                "Current group",
                teacherId
            );
            execute(
                connection,
                "INSERT INTO group_students (group_id,class_id,student_id,assigned_by) VALUES (?,?,?,?)",
                currentGroupId,
                currentClassId,
                studentId,
                teacherId
            );
            execute(
                connection,
                "INSERT INTO subjects (id,code,name) VALUES (?,?,?)",
                subjectId,
                "RANK_" + subjectId.toString().substring(0, 8),
                "Ranking subject " + subjectId
            );
            execute(
                connection,
                """
                INSERT INTO quizzes (
                    id, creator_id, subject_id, title, status,
                    max_questions_per_session, shuffle
                ) VALUES (?,?,?,?,'PUBLISHED',1,FALSE)
                """,
                quizId,
                teacherId,
                subjectId,
                "Historical ranking quiz"
            );
            execute(
                connection,
                """
                INSERT INTO sessions (
                    id, quiz_id, teacher_id, status, title_snapshot,
                    teacher_name_snapshot, quiz_snapshot, started_at, closes_at
                ) VALUES (?,?,?,'CLOSED',?,?,'{"questions":[]}'::jsonb,
                          CURRENT_TIMESTAMP - INTERVAL '2 hours',
                          CURRENT_TIMESTAMP - INTERVAL '1 hour')
                """,
                sessionId,
                quizId,
                teacherId,
                "Historical ranking quiz",
                "Ranking teacher"
            );
            execute(
                connection,
                "INSERT INTO session_classes (session_id,class_id,class_name) VALUES (?,?,?),(?,?,?)",
                sessionId,
                launchClassId,
                "Launch class",
                sessionId,
                currentClassId,
                "Current class"
            );
            execute(
                connection,
                "INSERT INTO session_groups (session_id,group_id,class_id,group_name) VALUES (?,?,?,?),(?,?,?,?)",
                sessionId,
                launchGroupId,
                launchClassId,
                "Launch group",
                sessionId,
                currentGroupId,
                currentClassId,
                "Current group"
            );
            execute(
                connection,
                "INSERT INTO session_participants (session_id,student_id,class_id) VALUES (?,?,?)",
                sessionId,
                studentId,
                launchClassId
            );
            execute(
                connection,
                "INSERT INTO session_participant_groups (session_id,student_id,group_id,class_id) VALUES (?,?,?,?)",
                sessionId,
                studentId,
                launchGroupId,
                launchClassId
            );
            execute(
                connection,
                """
                INSERT INTO attempts (
                    id, session_id, student_id, attempt_number, status,
                    started_at, submitted_at, score, max_score
                ) VALUES (?,?,?,1,'GRADED',CURRENT_TIMESTAMP - INTERVAL '2 hours',
                          CURRENT_TIMESTAMP - INTERVAL '1 hour',5,5)
                """,
                attemptId,
                sessionId,
                studentId
            );
        }

        var rankings = this.rankings.forStudent(studentId);

        assertThat(rankings).hasSize(2);
        assertThat(rankings)
            .allSatisfy(ranking -> {
                assertThat(ranking.members()).hasSize(1);
                assertThat(ranking.members().get(0).score()).isZero();
                assertThat(ranking.members().get(0).attemptCount()).isZero();
            });
    }

    @Test
    void sessionGroupCannotReferenceAGroupFromAnotherClass()
        throws SQLException {
        UUID administratorId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID firstClassId = UUID.randomUUID();
        UUID secondClassId = UUID.randomUUID();
        UUID firstClassGroupId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        try (Connection connection = POSTGRES.createConnection("")) {
            insertUser(connection, administratorId, "constraint-admin", uniqueUsername("admin"));
            insertUser(connection, teacherId, "constraint-teacher", uniqueUsername("teacher"));
            execute(
                connection,
                "INSERT INTO classes (id,name,created_by) VALUES (?,?,?),(?,?,?)",
                firstClassId,
                "Constraint A " + firstClassId,
                administratorId,
                secondClassId,
                "Constraint B " + secondClassId,
                administratorId
            );
            execute(
                connection,
                "INSERT INTO class_groups (id,class_id,name,created_by) VALUES (?,?,?,?)",
                firstClassGroupId,
                firstClassId,
                "Constraint group",
                teacherId
            );
            execute(
                connection,
                "INSERT INTO subjects (id,code,name) VALUES (?,?,?)",
                subjectId,
                "CONS_" + subjectId.toString().substring(0, 8),
                "Constraint subject " + subjectId
            );
            execute(
                connection,
                """
                INSERT INTO quizzes (
                    id, creator_id, subject_id, title, status,
                    max_questions_per_session, shuffle
                ) VALUES (?,?,?,?,'PUBLISHED',1,FALSE)
                """,
                quizId,
                teacherId,
                subjectId,
                "Constraint quiz"
            );
            execute(
                connection,
                """
                INSERT INTO sessions (
                    id, quiz_id, teacher_id, status, title_snapshot,
                    teacher_name_snapshot, quiz_snapshot, started_at, closes_at
                ) VALUES (?,?,?,'ACTIVE',?,?,'{"questions":[]}'::jsonb,
                          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 hour')
                """,
                sessionId,
                quizId,
                teacherId,
                "Constraint quiz",
                "Constraint teacher"
            );

            assertThatThrownBy(() ->
                execute(
                    connection,
                    "INSERT INTO session_groups (session_id,group_id,class_id,group_name) VALUES (?,?,?,?)",
                    sessionId,
                    firstClassGroupId,
                    secondClassId,
                    "Invalid target"
                )
            ).isInstanceOf(SQLException.class);
        }
    }

    private static void insertUser(
        Connection connection,
        UUID id,
        String subject,
        String username
    ) throws SQLException {
        execute(
            connection,
            "INSERT INTO users (id,issuer,subject,username,display_name) VALUES (?,?,?,?,?)",
            id,
            "https://issuer.example",
            subject,
            username,
            subject
        );
    }

    private static void execute(
        Connection connection,
        String sql,
        Object... values
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
        }
    }

    private static String uniqueUsername(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@school.cz";
    }
}
