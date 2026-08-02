package com.sosehl.curtis_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSubWithoutRolesForPlainUser() throws Exception {
        mockMvc
            .perform(
                get("/v1/me")
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("student1")
                            .roles()
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("student1"))
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles.length()").value(0));
    }

    @Test
    void shouldReturnTeacherRole() throws Exception {
        mockMvc
            .perform(
                get("/v1/me")
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("teacher1")
                            .roles("TEACHER")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("teacher1"))
            .andExpect(jsonPath("$.roles[0]").value("TEACHER"));
    }

    @Test
    void shouldReturnAllRoles() throws Exception {
        mockMvc
            .perform(
                get("/v1/me")
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("both")
                            .roles("TEACHER", "STUDENT")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles.length()").value(2))
            .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
            .andExpect(jsonPath("$.roles[1]").value("TEACHER"));
    }

    @Test
    void shouldRedirectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/v1/me")).andExpect(status().isFound());
    }
}
