package com.sosehl.curtis.platform.security.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sosehl.curtis.config.SecurityConfig;
import com.sosehl.curtis.platform.security.infrastructure.EntraOidcUserService;
import com.sosehl.curtis.shared.errors.GlobalExceptionHandler;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthorizationProbeController.class)
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class,
})
@ActiveProfiles("test")
class SecurityRouteAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EntraOidcUserService oidcUserService;

    @MockitoBean
    private Tracer tracer;

    @BeforeEach
    void provideActiveTrace() {
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("0123456789abcdef0123456789abcdef");
    }

    @Test
    void anonymousApiRequestUsesProblemDetails() throws Exception {
        mvc
            .perform(get("/v1/admin/probe"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string("Content-Language", "en"))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication is required."))
            .andExpect(jsonPath("$.code").value("authentication_required"))
            .andExpect(
                jsonPath("$.traceId").value(
                    "0123456789abcdef0123456789abcdef"
                )
            );
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotUseAdministratorRoutes() throws Exception {
        mvc
            .perform(get("/v1/admin/probe"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("access_denied"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorCanUseAdministratorRoutes() throws Exception {
        mvc.perform(get("/v1/admin/probe")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void mutationWithoutCsrfTokenIsRejected() throws Exception {
        mvc
            .perform(post("/v1/admin/probe"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("csrf_invalid"));

        mvc
            .perform(post("/v1/admin/probe").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void logoutUsesStandardNoContentResponse() throws Exception {
        mvc
            .perform(post("/logout").with(csrf()))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void validationErrorsRemainEnglishAndUseProblemDetails() throws Exception {
        mvc
            .perform(
                post("/v1/admin/probe/validate")
                    .with(csrf())
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "cs-CZ")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string("Content-Language", "en"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(
                jsonPath("$.detail").value(
                    "The request contains invalid fields."
                )
            )
            .andExpect(jsonPath("$.code").value("validation_failed"))
            .andExpect(jsonPath("$.fieldErrors.name").value("must not be blank"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void nullCollectionElementIsAValidationProblem() throws Exception {
        mvc
            .perform(
                post("/v1/admin/probe/validate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Valid\",\"values\":[null]}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("validation_failed"))
            .andExpect(jsonPath("$.fieldErrors['values[0]']").value("must not be null"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void missingMultipartPartIsAStandardBadRequest() throws Exception {
        mvc
            .perform(multipart("/v1/admin/probe/upload").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string("Content-Language", "en"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("missing_request_part"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void malformedJsonUsesSpringProblemDetailsMapping() throws Exception {
        mvc
            .perform(
                post("/v1/admin/probe/validate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{")
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("invalid_request_body"))
            .andExpect(
                jsonPath("$.detail").value(
                    "The request body is missing or malformed."
                )
            );
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void unsupportedMethodUsesSpringProblemDetailsMapping() throws Exception {
        mvc
            .perform(get("/v1/admin/probe/validate"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("method_not_allowed"));
    }

}
