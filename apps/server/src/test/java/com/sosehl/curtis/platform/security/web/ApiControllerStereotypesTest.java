package com.sosehl.curtis.platform.security.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

class ApiControllerStereotypesTest {

    @Test
    void administratorControllerIsSecuredAndDiscoverableBySpringMvc() {
        assertStereotype(SOSE_AdminApiController.class, "hasRole('ADMINISTRATOR')");
    }

    @Test
    void teacherControllerIsSecuredAndDiscoverableBySpringMvc() {
        assertStereotype(SOSE_TeacherApiController.class, "hasRole('TEACHER')");
    }

    @Test
    void studentControllerIsSecuredAndDiscoverableBySpringMvc() {
        assertStereotype(SOSE_StudentApiController.class, "hasRole('STUDENT')");
    }

    private static void assertStereotype(
        Class<?> annotation,
        String authorizationExpression
    ) {
        assertTrue(AnnotatedElementUtils.hasAnnotation(annotation, RestController.class));
        PreAuthorize authorization = AnnotatedElementUtils.findMergedAnnotation(
            annotation,
            PreAuthorize.class
        );
        assertNotNull(authorization);
        assertEquals(authorizationExpression, authorization.value());
    }
}
