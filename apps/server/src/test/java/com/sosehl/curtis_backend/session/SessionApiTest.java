package com.sosehl.curtis_backend.session;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.question.Question;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
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
}
