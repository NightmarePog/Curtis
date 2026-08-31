package com.sosehl.curtis_backend.question;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.Question;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizRepository;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class QuestionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private QuizRepository quizRepository;
    @Autowired private ObjectMapper objectMapper;

    private UUID quizUuid;

    @BeforeEach
    void setUp() {
        Quiz quiz = new Quiz();
        quiz.setTitle("Test kvíz");
        quiz.setMaxQuestionsPerSession(5);
        quiz.setShuffle(false);

        QuestionAnswer correct = new QuestionAnswer();
        correct.setAnswer("Ano");
        correct.setIsCorrect(true);
        QuestionAnswer wrong = new QuestionAnswer();
        wrong.setAnswer("Ne");
        wrong.setIsCorrect(false);

        Question question = new Question();
        question.setQuestion("Je Curtis fajn?");
        question.setAnswers(new ArrayList<>(List.of(correct, wrong)));
        question.setTimeInSeconds(30);
        question.setQuiz(quiz);

        quiz.setQuestions(new ArrayList<>(List.of(question)));
        quizRepository.save(quiz);
        quizUuid = quiz.getUuid();
    }

    @Test
    void returnsQuestionsWithoutEmbeddedQuiz() throws Exception {
        mockMvc
            .perform(
                get("/v1/quiz/{uuid}/questions", quizUuid)
                    .with(
                        oidcLogin()
                            .authorities(
                                new SimpleGrantedAuthority("ROLE_TEACHER")
                            )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].question").value("Je Curtis fajn?"))
            .andExpect(jsonPath("$[0].answers[0].answer").value("Ano"))
            .andExpect(jsonPath("$[0].answers[0].isCorrect").value(true))
            .andExpect(jsonPath("$[0].timeInSeconds").value(30))
            .andExpect(jsonPath("$[0].quiz").doesNotExist());
    }

    @Test
    void mapsNewQuestionFieldsThroughCreateAndQuizResponse() throws Exception {
        QuestionCreateDto request = new QuestionCreateDto();
        request.setQuestion("Match the terms");
        request.setType(QuestionType.MATCHING);
        request.setPoints(4);
        request.setTimeInSeconds(30);
        request.setCodeSnippet("const answer = 42;");
        request.setImageRef("diagram.png");
        request.setPairs(
            List.of(new MatchingPair("one", "1"), new MatchingPair("two", "2"))
        );

        mockMvc
            .perform(
                post("/v1/quiz/{uuid}/questions", quizUuid)
                    .with(
                        oidcLogin()
                            .authorities(
                                new SimpleGrantedAuthority("ROLE_TEACHER")
                            )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());

        mockMvc
            .perform(
                get("/v1/quiz/{uuid}/questions", quizUuid).with(
                    oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_TEACHER"))
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1].type").value("MATCHING"))
            .andExpect(jsonPath("$[1].points").value(4))
            .andExpect(
                jsonPath("$[1].codeSnippet").value("const answer = 42;")
            )
            .andExpect(jsonPath("$[1].imageRef").value("diagram.png"))
            .andExpect(jsonPath("$[1].pairs[0].left").value("one"))
            .andExpect(jsonPath("$[1].pairs[0].right").value("1"));
    }
}
