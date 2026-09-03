package com.sosehl.curtis.platform.security.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Curtis stereotype for an administrator-only Spring MVC controller.
 *
 * <p>This annotation combines {@link RestController} with the class-level
 * authorization rule {@code @PreAuthorize("hasRole('ADMINISTRATOR')")}. Under
 * Curtis's default Spring Security role prefix, every handler method without
 * its own method-security annotation therefore requires the
 * {@code ROLE_ADMINISTRATOR} authority. A method-level security annotation
 * replaces this class-level rule. {@code SecurityConfig}'s
 * {@code @EnableMethodSecurity} activates method security, and Curtis defines
 * no role hierarchy: administrator does not implicitly grant teacher or
 * student access.</p>
 *
 * <p>The annotation deliberately does not define a URL, HTTP method, response
 * content type, or current-user argument. The controller must declare its own
 * request mappings, and it can use {@link SOSE_CurrentUser} when it needs the
 * current database-backed Curtis user and account-freshness checks.
 * Request-level rules in {@code SecurityConfig} remain a separate,
 * default-deny outer security boundary; this stereotype does not whitelist an
 * otherwise denied URL.</p>
 *
 * <p>Use this only on controllers whose complete HTTP surface is intended for
 * administrators.</p>
 *
 * @see SOSE_TeacherApiController
 * @see SOSE_StudentApiController
 * @see SOSE_CurrentUser
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
@PreAuthorize("hasRole('ADMINISTRATOR')")
public @interface SOSE_AdminApiController {}
