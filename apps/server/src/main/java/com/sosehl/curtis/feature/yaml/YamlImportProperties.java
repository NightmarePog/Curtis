package com.sosehl.curtis.feature.yaml;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.quiz-import")
public record YamlImportProperties(
    @DefaultValue("false") boolean enabled,
    @NotNull @DefaultValue("/data/import") Path root,
    @Positive @DefaultValue("5000") long pollMs,
    @Positive @DefaultValue("20") int maxAssets,
    @Positive @DefaultValue("52428800") long maxTotalAssetBytes
) {}
