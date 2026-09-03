package com.sosehl.curtis.feature.media;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.media")
public record MediaProperties(
    @NotNull @DefaultValue("/data/media") Path root,
    @Positive @DefaultValue("10485760") long maxBytes,
    @Positive @DefaultValue("40000000") long maxPixels
) {}
