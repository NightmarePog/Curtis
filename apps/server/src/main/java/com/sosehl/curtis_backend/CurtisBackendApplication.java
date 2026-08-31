package com.sosehl.curtis_backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CurtisBackendApplication {

    public static void main(String[] args) {
        loadEnvProperties();

        SpringApplication app = new SpringApplication(
            CurtisBackendApplication.class
        );

        app.run(args);
    }

    private static void loadEnvProperties() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String[] keys = {
            "MICROSOFT_CLIENT_ID",
            "MICROSOFT_CLIENT_SECRET",
            "MICROSOFT_TENANT_ID",
            "MICROSOFT_REDIRECT_URI",
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
        };

        for (String key : keys) {
            String value = dotenv.get(key);
            if (value != null && !value.isBlank()) {
                String springKey = key.equals("MICROSOFT_CLIENT_ID")
                    ? "spring.security.oauth2.client.registration.microsoft.client-id"
                    : key.equals("MICROSOFT_CLIENT_SECRET")
                    ? "spring.security.oauth2.client.registration.microsoft.client-secret"
                    : key.equals("MICROSOFT_TENANT_ID")
                    ? "microsoft.tenant-id"
                    : key.equals("MICROSOFT_REDIRECT_URI")
                    ? "spring.security.oauth2.client.registration.microsoft.redirect-uri"
                    : key.equals("SPRING_DATASOURCE_URL")
                    ? "spring.datasource.url"
                    : key.equals("SPRING_DATASOURCE_USERNAME")
                    ? "spring.datasource.username"
                    : key.equals("SPRING_DATASOURCE_PASSWORD")
                    ? "spring.datasource.password"
                    : null;

                if (springKey != null) {
                    System.setProperty(springKey, value);
                }
            }
        }
    }
}
