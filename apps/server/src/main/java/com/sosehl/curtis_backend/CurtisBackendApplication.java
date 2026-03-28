package com.sosehl.curtis_backend;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CurtisBackendApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String clientSecret = dotenv.get("CLIENT_SECRET");
        if (clientSecret != null) {
            System.setProperty(
                "spring.security.oauth2.client.registration.microsoft.client-secret",
                clientSecret
            );
        }

        SpringApplication app = new SpringApplication(
            CurtisBackendApplication.class
        );

        Properties props = new Properties();
        props.setProperty(
            "spring.datasource.url",
            "jdbc:postgresql://localhost:5432/curtisdb"
        );
        props.setProperty("spring.datasource.username", "curtisuser");
        props.setProperty("spring.datasource.password", "curtispass");
        props.setProperty(
            "spring.datasource.driver-class-name",
            "org.postgresql.Driver"
        );
        props.setProperty("spring.jpa.hibernate.ddl-auto", "update");

        app.setDefaultProperties(props);
        app.run(args);
    }
}
