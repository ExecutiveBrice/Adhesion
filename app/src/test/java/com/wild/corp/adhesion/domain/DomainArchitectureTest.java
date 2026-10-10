package com.wild.corp.adhesion.domain;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DomainArchitectureTest {
    @Test
    void decisionsDoNotDependOnApplicationServicesRepositoriesControllersOrSpring() throws Exception {
        Path root = Path.of("src/main/java/com/wild/corp/adhesion/domain");
        try (var sources = Files.walk(root)) {
            var files = sources.filter(path -> path.toString().endsWith(".java")).toList();
            assertThat(files).isNotEmpty();
            for (Path file : files) {
                String source = Files.readString(file);
                assertThat(source).as("Frontière métier de %s", file)
                        .doesNotContain("com.wild.corp.adhesion.repository.", "com.wild.corp.adhesion.services.",
                                "com.wild.corp.adhesion.controllers.", "org.springframework.",
                                "com.wild.corp.adhesion.shop.api.", ".repository.", ".service.");
            }
        }
    }
}
