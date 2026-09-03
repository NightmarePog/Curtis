package com.sosehl.curtis.platform.security.web;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/** Resolves and verifies the current Curtis user from the OIDC principal. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(
    expression = "@currentUserService.require(#this)",
    errorOnInvalidType = true
)
@Parameter(hidden = true)
public @interface SOSE_CurrentUser {}
