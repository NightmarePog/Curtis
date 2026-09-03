package com.sosehl.curtis.feature.quizzes;

import static org.assertj.core.api.Assertions.assertThat;

import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.PairResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.PairWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class QuizReplacementPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        "postgres:16-alpine"
    );

    private static final UUID TEACHER_ID = UUID.fromString(
        "781a57d1-3f5a-4da8-966c-eae82b59b8d8"
    );
    private static final UUID SUBJECT_ID = UUID.fromString(
        "47fc22d4-c1ec-4685-89a8-98d52183e535"
    );

    @Autowired
    private QuizService quizzes;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void seedTeacherAndSubjectAssignment() {
        jdbc.update(
            """
            INSERT INTO users (id, issuer, subject, username, display_name)
            VALUES (?, 'test-issuer', 'replacement-teacher', 'teacher@example.test', 'Teacher')
            ON CONFLICT (id) DO NOTHING
            """,
            TEACHER_ID
        );
        jdbc.update(
            """
            INSERT INTO user_roles (user_id, role)
            VALUES (?, 'TEACHER')
            ON CONFLICT DO NOTHING
            """,
            TEACHER_ID
        );
        jdbc.update(
            """
            INSERT INTO subjects (id, code, name)
            VALUES (?, 'RPL', 'Replacement testing')
            ON CONFLICT (id) DO NOTHING
            """,
            SUBJECT_ID
        );
        jdbc.update(
            """
            INSERT INTO teacher_subjects (teacher_id, subject_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """,
            TEACHER_ID,
            SUBJECT_ID
        );
    }

    @Test
    void replacesNestedContentWhilePreservingQuestionOptionAndPairIds() {
        QuizResponse created = quizzes.createForTeacher(
            TEACHER_ID,
            initialRequest()
        );
        QuestionResponse choice = created.questions().get(0);
        QuestionResponse matching = created.questions().get(1);

        QuizResponse replaced = quizzes.replaceForTeacher(
            TEACHER_ID,
            created.id(),
            replacementRequest(created, choice, matching)
        );
        QuizResponse reloaded = quizzes.getForTeacher(TEACHER_ID, created.id());

        assertThat(replaced.version()).isGreaterThan(created.version());
        assertThat(reloaded.version()).isEqualTo(replaced.version());
        assertThat(reloaded.title()).isEqualTo("Updated quiz");
        assertThat(reloaded.questions())
            .extracting(QuestionResponse::id)
            .containsExactly(choice.id(), matching.id());

        QuestionResponse reloadedChoice = reloaded.questions().get(0);
        assertThat(reloadedChoice.prompt()).isEqualTo("Choose the updated answer");
        assertThat(reloadedChoice.codeSnippet()).isEqualTo("updated();");
        assertThat(reloadedChoice.options())
            .extracting(OptionResponse::id)
            .containsExactly(choice.options().get(0).id(), choice.options().get(1).id());
        assertThat(reloadedChoice.options())
            .extracting(OptionResponse::text)
            .containsExactly("Updated first option", "Updated second option");
        assertThat(reloadedChoice.options())
            .extracting(OptionResponse::correct)
            .containsExactly(true, false);

        QuestionResponse reloadedMatching = reloaded.questions().get(1);
        assertThat(reloadedMatching.prompt()).isEqualTo("Match the updated pairs");
        assertThat(reloadedMatching.pairs())
            .extracting(PairResponse::id)
            .containsExactly(matching.pairs().get(0).id(), matching.pairs().get(1).id());
        assertThat(reloadedMatching.pairs())
            .extracting(PairResponse::left, PairResponse::right)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("updated-left-1", "updated-right-1"),
                org.assertj.core.groups.Tuple.tuple("updated-left-2", "updated-right-2")
            );

        assertThat(count("questions", "quiz_id", created.id())).isEqualTo(2);
        assertThat(count("question_options", "question_id", choice.id())).isEqualTo(2);
        assertThat(count("matching_pairs", "question_id", matching.id())).isEqualTo(2);
    }

    private QuizWriteRequest initialRequest() {
        return new QuizWriteRequest(
            "Original quiz",
            "Original description",
            SUBJECT_ID,
            "Original chapter",
            QuizStatus.DRAFT,
            2,
            false,
            null,
            null,
            null,
            List.of(
                new QuestionWriteRequest(
                    null,
                    QuestionType.MULTIPLE_CHOICE,
                    "Choose the original answer",
                    2,
                    "original();",
                    null,
                    30,
                    List.of(
                        new OptionWriteRequest(null, "Original first option", false),
                        new OptionWriteRequest(null, "Original second option", true)
                    ),
                    List.of()
                ),
                new QuestionWriteRequest(
                    null,
                    QuestionType.MATCHING,
                    "Match the original pairs",
                    4,
                    null,
                    null,
                    45,
                    List.of(),
                    List.of(
                        new PairWriteRequest(null, "original-left-1", "original-right-1"),
                        new PairWriteRequest(null, "original-left-2", "original-right-2")
                    )
                )
            )
        );
    }

    private QuizWriteRequest replacementRequest(
        QuizResponse created,
        QuestionResponse choice,
        QuestionResponse matching
    ) {
        return new QuizWriteRequest(
            "Updated quiz",
            "Updated description",
            SUBJECT_ID,
            "Updated chapter",
            QuizStatus.DRAFT,
            2,
            true,
            null,
            null,
            created.version(),
            List.of(
                new QuestionWriteRequest(
                    choice.id(),
                    QuestionType.MULTIPLE_CHOICE,
                    "Choose the updated answer",
                    3,
                    "updated();",
                    null,
                    35,
                    List.of(
                        new OptionWriteRequest(
                            choice.options().get(0).id(),
                            "Updated first option",
                            true
                        ),
                        new OptionWriteRequest(
                            choice.options().get(1).id(),
                            "Updated second option",
                            false
                        )
                    ),
                    List.of()
                ),
                new QuestionWriteRequest(
                    matching.id(),
                    QuestionType.MATCHING,
                    "Match the updated pairs",
                    5,
                    null,
                    null,
                    50,
                    List.of(),
                    List.of(
                        new PairWriteRequest(
                            matching.pairs().get(0).id(),
                            "updated-left-1",
                            "updated-right-1"
                        ),
                        new PairWriteRequest(
                            matching.pairs().get(1).id(),
                            "updated-left-2",
                            "updated-right-2"
                        )
                    )
                )
            )
        );
    }

    private int count(String table, String ownerColumn, UUID ownerId) {
        Integer value = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + ownerColumn + " = ?",
            Integer.class,
            ownerId
        );
        return value == null ? 0 : value;
    }
}
