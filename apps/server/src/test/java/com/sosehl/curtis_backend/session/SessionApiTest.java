package com.sosehl.curtis_backend.session;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.question.Question;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class SessionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    private UUID quizUuid;

    @BeforeEach
    void setUp() {
        Quiz quiz = new Quiz();
        quiz.setTitle("API Quiz");
        quiz.setMaxQuestionsPerSession(5);
        quiz.setShuffle(false);

        QuestionAnswer correct = new QuestionAnswer();
        correct.setAnswer("Java");
        correct.setIsCorrect(true);
        QuestionAnswer wrong = new QuestionAnswer();
        wrong.setAnswer("Python");
        wrong.setIsCorrect(false);

        Question question = new Question();
        question.setQuestion("Co je Java?");
        question.setAnswers(new java.util.ArrayList<>(List.of(correct, wrong)));
        question.setTimeInSeconds(10);
        question.setQuiz(quiz);
        quiz.setQuestions(new java.util.ArrayList<>(List.of(question)));

        quizRepository.save(quiz);
        quizUuid = quiz.getUuid();
    }

    @Test
    void shouldPlayFullSessionAndHideCorrectAnswers() throws Exception {
        MvcResult sessionResult = mockMvc
            .perform(
                post("/v1/sessions")
                    .with(
                        oidcLogin()
                            .authorities(
                                new SimpleGrantedAuthority("ROLE_TEACHER")
                            )
                    )
                    .param("quizUuid", quizUuid.toString())
            )
            .andExpect(status().isCreated())
            .andReturn();
        UUID sessionUuid = objectMapper
            .readValue(
                sessionResult.getResponse().getContentAsString(),
                UUID.class
            );

        MvcResult joinResult = mockMvc
            .perform(
                post("/v1/sessions/" + sessionUuid + "/join")
                    .with(oidcLogin())
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.answers[0].isCorrect")
                    .value(org.hamcrest.Matchers.nullValue())
            )
            .andReturn();

        String question = objectMapper
            .readTree(joinResult.getResponse().getContentAsString())
            .get("question")
            .asText();
        org.assertj.core.api.Assertions.assertThat(question).isEqualTo("Co je Java?");

        mockMvc
            .perform(
                post("/v1/sessions/" + sessionUuid + "/next")
                    .with(oidcLogin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[0]")
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(
                post("/v1/sessions/" + sessionUuid + "/finish")
                    .with(oidcLogin())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(1))
            .andExpect(jsonPath("$.maxScore").value(1));
    }

    @Test
    void shouldPersistPendingFreeTextAndAllowTeacherToGradeIt() throws Exception {
        Quiz freeTextQuiz = new Quiz();
        freeTextQuiz.setTitle("Free text API quiz");
        freeTextQuiz.setMaxQuestionsPerSession(5);
        freeTextQuiz.setShuffle(false);
        Question freeText = new Question();
        freeText.setQuestion("Explain Java");
        freeText.setType(QuestionType.FREE_TEXT);
        freeText.setPoints(3);
        freeText.setTimeInSeconds(30);
        freeText.setQuiz(freeTextQuiz);
        freeTextQuiz.setQuestions(new java.util.ArrayList<>(List.of(freeText)));
        quizRepository.save(freeTextQuiz);

        MvcResult sessionResult = mockMvc
            .perform(
                post("/v1/sessions")
                    .with(
                        oidcLogin().authorities(
                            new SimpleGrantedAuthority("ROLE_TEACHER")
                        )
                    )
                    .param("quizUuid", freeTextQuiz.getUuid().toString())
            )
            .andExpect(status().isCreated())
            .andReturn();
        UUID sessionUuid = objectMapper.readValue(
            sessionResult.getResponse().getContentAsString(),
            UUID.class
        );

        mockMvc
            .perform(post("/v1/sessions/{id}/join", sessionUuid).with(oidcLogin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("FREE_TEXT"));

        mockMvc
            .perform(
                post("/v1/sessions/{id}/next", sessionUuid)
                    .with(oidcLogin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"FREE_TEXT\",\"text\":\"Java runs on a VM\"}")
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(post("/v1/sessions/{id}/finish", sessionUuid).with(oidcLogin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(0))
            .andExpect(jsonPath("$.maxScore").value(3));

        MvcResult pending = mockMvc
            .perform(
                get("/v1/sessions/{id}/pending-text-answers", sessionUuid)
                    .with(
                        oidcLogin().authorities(
                            new SimpleGrantedAuthority("ROLE_TEACHER")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].text").value("Java runs on a VM"))
            .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"))
            .andReturn();
        long resultId = objectMapper
            .readTree(pending.getResponse().getContentAsString())
            .get(0)
            .get("resultId")
            .asLong();

        mockMvc
            .perform(
                post("/v1/sessions/{id}/text-answers/{resultId}/grade", sessionUuid, resultId)
                    .with(
                        oidcLogin().authorities(
                            new SimpleGrantedAuthority("ROLE_TEACHER")
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"awardedPoints\":2}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("GRADED"))
            .andExpect(jsonPath("$.awardedPoints").value(2));

        mockMvc
            .perform(
                get("/v1/sessions/{id}/results", sessionUuid)
                    .with(
                        oidcLogin().authorities(
                            new SimpleGrantedAuthority("ROLE_TEACHER")
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].score").value(2));
    }

    @Test
    void shouldAutoGradeMatchingSubmissionThroughApi() throws Exception {
        Quiz matchingQuiz = new Quiz();
        matchingQuiz.setTitle("Matching API quiz");
        matchingQuiz.setMaxQuestionsPerSession(5);
        matchingQuiz.setShuffle(false);
        Question matching = new Question();
        matching.setQuestion("Match the terms");
        matching.setType(QuestionType.MATCHING);
        matching.setPoints(2);
        matching.setPairs(
            new java.util.ArrayList<>(
                List.of(new MatchingPair("one", "1"), new MatchingPair("two", "2"))
            )
        );
        matching.setQuiz(matchingQuiz);
        matchingQuiz.setQuestions(new java.util.ArrayList<>(List.of(matching)));
        quizRepository.save(matchingQuiz);

        MvcResult sessionResult = mockMvc
            .perform(
                post("/v1/sessions")
                    .with(
                        oidcLogin().authorities(
                            new SimpleGrantedAuthority("ROLE_TEACHER")
                        )
                    )
                    .param("quizUuid", matchingQuiz.getUuid().toString())
            )
            .andExpect(status().isCreated())
            .andReturn();
        UUID sessionUuid = objectMapper.readValue(
            sessionResult.getResponse().getContentAsString(),
            UUID.class
        );

        mockMvc
            .perform(post("/v1/sessions/{id}/join", sessionUuid).with(oidcLogin()))
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/v1/sessions/{id}/next", sessionUuid)
                    .with(oidcLogin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"type\":\"MATCHING\",\"pairs\":[{" +
                        "\"leftIndex\":0,\"rightIndex\":0},{" +
                        "\"leftIndex\":1,\"rightIndex\":1}]}"
                    )
            )
            .andExpect(status().isBadRequest());

        mockMvc
            .perform(post("/v1/sessions/{id}/finish", sessionUuid).with(oidcLogin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(2))
            .andExpect(jsonPath("$.maxScore").value(2));
    }
}
