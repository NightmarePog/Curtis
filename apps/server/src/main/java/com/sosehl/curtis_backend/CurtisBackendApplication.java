package com.sosehl.curtis_backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CurtisBackendApplication {

    public static void main(String[] args) {
        MicrosoftOauthProperties();

        SpringApplication app = new SpringApplication(
            CurtisBackendApplication.class
        );

        app.run(args);
    }

    private static void MicrosoftOauthProperties() {
        Dotenv dotenv = Dotenv.load();
        String clientSecret = dotenv.get("CLIENT_SECRET");
        System.setProperty(
            "spring.security.oauth2.client.registration.microsoft.client-secret",
            clientSecret
        );

        System.out.println(
            System.getProperty(
                "spring.security.oauth2.client.registration.microsoft.client-secret"
            )
        );
    }
}
