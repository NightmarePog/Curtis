package com.sosehl.curtis;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.util.FileSystemUtils;

class ApplicationModulesTest {

    private static final String ROOT_PACKAGE = "com.sosehl.curtis";
    private static final String FEATURE_PACKAGE = "com.sosehl.curtis.feature";
    private static final DescribedPredicate<JavaClass> FEATURE_INTERNAL =
        DescribedPredicate.describe(
            "reside in a feature implementation package",
            ApplicationModulesTest::isFeatureInternal
        );
    private static final DescribedPredicate<JavaClass> TRANSPORT_TYPE =
        DescribedPredicate.describe(
            "have a transport-model name",
            ApplicationModulesTest::hasTransportTypeName
        );
    private static final Set<String> FEATURE_MODULES = Set.of(
        "classrooms",
        "media",
        "quizzes",
        "sessions",
        "subjects",
        "users",
        "yaml"
    );
    private static final Set<String> GENERIC_LAYER_PACKAGES = Set.of(
        "adapter",
        "adapters",
        "api",
        "application",
        "usecase",
        "usecases"
    );

    @Test
    void keepsFeatureFirstCleanArchitectureLayout() throws IOException {
        Path featureRoot = Path.of(
            "src",
            "main",
            "java",
            "com",
            "sosehl",
            "curtis",
            "feature"
        );
        assertFeatureLayout(featureRoot);
        assertNoHandwrittenModulithMetadata(featureRoot);
        assertNoJavaSources(
            Path.of(
                "src",
                "main",
                "java",
                "com",
                "sosehl",
                "curtis",
                "features"
            )
        );
    }

    @Test
    void keepsCoreIndependentFromFrameworksAndOuterLayers() {
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT_PACKAGE);

        noClasses()
            .that()
            .resideInAPackage("..feature..core..")
            .and()
            .doNotHaveSimpleName("package-info")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.servlet..",
                "jakarta.validation..",
                "com.fasterxml.jackson..",
                "org.hibernate..",
                "io.swagger.v3..",
                "..platform..",
                "..shared.errors.."
            )
            .because("feature cores must remain framework-independent")
            .check(classes);

        assertTransportTypesStayInDtoPackages(classes);

        noClasses()
            .that()
            .resideInAPackage("..feature..core..")
            .should()
            .dependOnClassesThat(FEATURE_INTERNAL)
            .because("feature cores must not depend on feature implementations")
            .check(classes);

        noClasses()
            .that()
            .resideInAPackage("..platform..")
            .should()
            .dependOnClassesThat(FEATURE_INTERNAL)
            .because("platform code uses only feature core contracts")
            .check(classes);
    }

    private void assertTransportTypesStayInDtoPackages(JavaClasses classes) {
        classes()
            .that()
            .resideInAnyPackage("..feature..", "..platform..")
            .and(TRANSPORT_TYPE)
            .should()
            .resideInAPackage("..dto..")
            .because("transport models belong in capability-local dto packages")
            .check(classes);
    }

    private static boolean hasTransportTypeName(JavaClass type) {
        return Stream.of(
            "Request",
            "Requests",
            "Response",
            "Responses",
            "Dto",
            "Dtos",
            "Document"
        ).anyMatch(type.getSimpleName()::endsWith);
    }

    @Test
    void verifiesFeatureModulesAndWritesArchitectureDocumentation() throws IOException {
        ApplicationModules modules = ApplicationModules.of(FEATURE_PACKAGE);

        assertEquals(FEATURE_MODULES.size(), modules.stream().count());
        FEATURE_MODULES.forEach(module ->
            assertTrue(modules.getModuleByName(module).isPresent(), () -> "Missing module: " + module)
        );

        modules.verify();

        Path documentation = Path.of("build", "spring-modulith-docs");
        FileSystemUtils.deleteRecursively(documentation);
        new Documenter(modules).writeDocumentation();

        assertTrue(Files.isRegularFile(documentation.resolve("components.puml")));
        assertTrue(Files.isRegularFile(documentation.resolve("all-docs.adoc")));
        FEATURE_MODULES.forEach(module -> {
            assertTrue(Files.isRegularFile(documentation.resolve("module-" + module + ".puml")));
            assertTrue(Files.isRegularFile(documentation.resolve("module-" + module + ".adoc")));
        });
        try (Stream<Path> generated = Files.list(documentation)) {
            assertEquals((FEATURE_MODULES.size() + 1L) * 2L, generated.count());
        }
    }

    private void assertFeatureLayout(Path featureRoot) throws IOException {
        try (Stream<Path> directories = Files.list(featureRoot)) {
            Set<String> modules = directories
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toSet());
            assertEquals(
                FEATURE_MODULES,
                modules,
                "Source feature directories must match the registered modules"
            );
        }

        try (Stream<Path> sources = Files.walk(featureRoot)) {
            var misplacedSources = sources
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(featureRoot::relativize)
                .filter(path ->
                    path.getNameCount() < 2 ||
                    !FEATURE_MODULES.contains(path.getName(0).toString()) ||
                    containsGenericLayerPackage(path)
                )
                .toList();

            assertTrue(
                misplacedSources.isEmpty(),
                () ->
                    "Feature sources must live beneath a registered module and " +
                    "use capability-oriented subpackages: " +
                    misplacedSources
            );
        }
    }

    private void assertNoJavaSources(Path root) throws IOException {
        if (Files.notExists(root)) {
            return;
        }
        try (Stream<Path> sources = Files.walk(root)) {
            assertTrue(
                sources.noneMatch(path ->
                    Files.isRegularFile(path) &&
                    path.getFileName().toString().endsWith(".java")
                ),
                "The legacy plural features package must not return"
            );
        }
    }

    private void assertNoHandwrittenModulithMetadata(Path root)
        throws IOException {
        try (Stream<Path> sources = Files.walk(root)) {
            var metadata = sources
                .filter(Files::isRegularFile)
                .filter(path ->
                    path.getFileName().toString().equals("package-info.java")
                )
                .toList();
            assertTrue(
                metadata.isEmpty(),
                () -> "Feature package metadata must be generated by Gradle: " +
                    metadata
            );
        }
    }

    private static boolean isFeatureInternal(JavaClass type) {
        String prefix = FEATURE_PACKAGE + ".";
        String packageName = type.getPackageName();
        if (!packageName.startsWith(prefix)) {
            return false;
        }
        String[] segments = packageName.substring(prefix.length()).split("\\.");
        return segments.length < 2 || !segments[1].equals("core");
    }

    private static boolean containsGenericLayerPackage(Path source) {
        for (int index = 1; index < source.getNameCount() - 1; index++) {
            if (GENERIC_LAYER_PACKAGES.contains(source.getName(index).toString())) {
                return true;
            }
        }
        return false;
    }
}
