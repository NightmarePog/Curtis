package com.sosehl.curtis_backend.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public class QuizMvcHelper {

    public static ResultActions postQuiz(
        MockMvc mockMvc,
        ObjectMapper mapper,
        QuizCreateRequest quiz
    ) throws Exception {
        return mockMvc.perform(
            post("/quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(quiz))
        );
    }

    public static ResultActions getQuiz(MockMvc mockMvc, UUID quizId)
        throws Exception {
        return mockMvc.perform(
            get("/quiz/{uuid}", quizId).contentType(MediaType.APPLICATION_JSON)
        );
    }

    public static ResultActions getAllQuizzes(MockMvc mockMvc)
        throws Exception {
        return mockMvc.perform(
            get("/quiz").contentType(MediaType.APPLICATION_JSON)
        );
    }

    public static ResultActions patchQuiz(MockMvc mockMvc, UUID quizId)
        throws Exception {
        return mockMvc.perform(
            patch("/quiz/{uuid}", quizId).contentType(
                MediaType.APPLICATION_JSON
            )
        );
    }
}
