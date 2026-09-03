package com.sosehl.curtis.documentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuizSchemaGenerationTest {

    @Test
    void publishesTheStrictValidatedYamlContract() throws IOException {
        JsonNode schema = new ObjectMapper().readTree(
            Path.of(
                System.getProperty(
                    "quiz.schema.path",
                    "build/schema/quiz.schema.json"
                )
            ).toFile()
        );

        assertEquals(
            "https://json-schema.org/draft/2020-12/schema",
            schema.path("$schema").textValue()
        );
        assertRejectsAdditionalProperties(schema);
        assertEquals(
            Set.of("title", "subjectId", "maxQuestionsPerSession", "questions"),
            textValues(schema.path("required"))
        );

        JsonNode properties = schema.path("properties");
        assertEquals(1, properties.path("title").path("minLength").intValue());
        assertEquals(100, properties.path("title").path("maxLength").intValue());
        assertEquals(
            1,
            properties.path("maxQuestionsPerSession").path("minimum").intValue()
        );
        assertEquals(
            100,
            properties.path("maxQuestionsPerSession").path("maximum").intValue()
        );
        assertEquals(1, properties.path("questions").path("minItems").intValue());
        assertEquals(100, properties.path("questions").path("maxItems").intValue());
        assertRejectsAdditionalProperties(
            properties.path("questions").path("items")
        );
    }

    private static Set<String> textValues(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(value -> values.add(value.textValue()));
        return values;
    }

    private static void assertRejectsAdditionalProperties(JsonNode objectSchema) {
        JsonNode additionalProperties = objectSchema.path("additionalProperties");
        assertTrue(additionalProperties.isBoolean());
        assertFalse(additionalProperties.booleanValue());
    }
}
