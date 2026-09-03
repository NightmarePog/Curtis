package com.sosehl.curtis.documentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

/** Build-time entry point for the quiz YAML document's editor schema. */
public final class QuizSchemaGenerator {

    private static final int EXPECTED_ARGUMENTS = 2;

    private QuizSchemaGenerator() {}

    public static void main(String[] arguments)
        throws ClassNotFoundException, IOException {
        if (arguments.length != EXPECTED_ARGUMENTS) {
            throw new IllegalArgumentException(
                "Expected the document class name and output path"
            );
        }

        Class<?> documentType = Class.forName(arguments[0]);
        Path output = Path.of(arguments[1]);
        SchemaGeneratorConfigBuilder configuration =
            new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON
            )
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(new JacksonModule())
                .with(
                    new JakartaValidationModule(
                        JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                        JakartaValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED
                    )
                );
        JsonNode schema = new SchemaGenerator(
            configuration.build()
        ).generateSchema(documentType);
        ObjectMapper objectMapper = new ObjectMapper();

        Files.createDirectories(output.getParent());
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(output.toFile(), canonicalize(schema, objectMapper));
    }

    private static JsonNode canonicalize(JsonNode node, ObjectMapper objectMapper) {
        if (node.isObject()) {
            var names = new ArrayList<String>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            var object = objectMapper.createObjectNode();
            names.forEach(name ->
                object.set(name, canonicalize(node.get(name), objectMapper))
            );
            return object;
        }
        if (node.isArray()) {
            var array = objectMapper.createArrayNode();
            node.forEach(element -> array.add(canonicalize(element, objectMapper)));
            return array;
        }
        return node;
    }
}
