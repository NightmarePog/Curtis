package com.sosehl.curtis_backend.domain.v1.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SnapshotUtil {

    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(
            SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
            true
        );

    public static String toJson(Quiz quiz) {
        try {
            return mapper.writeValueAsString(quiz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
