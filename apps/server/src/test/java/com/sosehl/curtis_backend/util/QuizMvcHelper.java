package com.sosehl.curtis_backend.util;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class QuizMvcHelper {

    public static MockHttpServletRequestBuilder postQuiz(
        ObjectMapper mapper,
        QuizCreateRequest quiz
    ) throws Exception {
        return post("/quiz")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(quiz));
    }

    public static MockHttpServletRequestBuilder getQuiz(UUID quizId) {
        return get("/quiz/{uuid}", quizId).contentType(
            MediaType.APPLICATION_JSON
        );
    }

    public static MockHttpServletRequestBuilder getAllQuizzes() {
        return get("/quiz").contentType(MediaType.APPLICATION_JSON);
    }

    public static MockHttpServletRequestBuilder patchQuiz(
        ObjectMapper mapper,
        QuizPatchRequest quiz
    ) throws Exception {
        return patch("/quiz")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(quiz));
    }
}
