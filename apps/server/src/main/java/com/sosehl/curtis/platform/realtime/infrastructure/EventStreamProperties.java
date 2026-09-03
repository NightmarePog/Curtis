package com.sosehl.curtis.platform.realtime.infrastructure;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.events")
public record EventStreamProperties(
    @PositiveOrZero @DefaultValue("0") long connectionTimeoutMs,
    @Positive @DefaultValue("25000") long heartbeatMs
) {}
