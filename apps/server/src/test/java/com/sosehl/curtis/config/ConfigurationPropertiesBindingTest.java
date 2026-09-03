package com.sosehl.curtis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sosehl.curtis.CurtisBackendApplication;
import com.sosehl.curtis.feature.media.MediaProperties;
import com.sosehl.curtis.feature.yaml.YamlImportProperties;
import com.sosehl.curtis.platform.realtime.infrastructure.EventStreamProperties;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ConfigurationPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ConfigurationPropertiesAutoConfiguration.class,
                ValidationAutoConfiguration.class
            )
        )
        .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void scansAndBindsTypedApplicationSettings() {
        contextRunner
            .withPropertyValues(
                "app.media.root=/tmp/curtis-media",
                "app.media.max-bytes=2048",
                "app.media.max-pixels=4096",
                "app.quiz-import.enabled=true",
                "app.quiz-import.root=/tmp/curtis-import",
                "app.quiz-import.poll-ms=750",
                "app.quiz-import.max-assets=3",
                "app.quiz-import.max-total-asset-bytes=8192",
                "app.events.connection-timeout-ms=1234",
                "app.events.heartbeat-ms=4321"
            )
            .run(context -> {
                assertThat(context.getStartupFailure()).isNull();
                assertThat(context.getBean(MediaProperties.class).root())
                    .isEqualTo(Path.of("/tmp/curtis-media"));
                assertThat(context.getBean(YamlImportProperties.class).pollMs())
                    .isEqualTo(750);
                assertThat(
                    context.getBean(EventStreamProperties.class).connectionTimeoutMs()
                ).isEqualTo(1234);
            });
    }

    @Test
    void rejectsNonPositiveMediaLimits() {
        contextRunner
            .withPropertyValues("app.media.max-bytes=0")
            .run(context ->
                assertThat(context.getStartupFailure())
                    .isNotNull()
                    .hasStackTraceContaining("must be greater than 0")
            );
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = CurtisBackendApplication.class)
    static class PropertiesConfiguration {}
}
