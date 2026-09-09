package com.wild.corp.adhesion.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadControllerTest {

    @TempDir
    Path storageDirectory;

    @Test
    void savesAndReplacesAnAdherentDocument() throws Exception {
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "imageStorageDir", storageDirectory);

        controller.uploadFile("42", pdf("certificat.pdf", "premier contenu"));
        controller.uploadFile("42", pdf("certificat.pdf", "court"));

        Path savedDocument = storageDirectory.resolve("42/certificat.pdf");
        assertThat(Files.readString(savedDocument)).isEqualTo("court");
    }

    @Test
    void rejectsANonNumericAdherentIdAsABadRequest() {
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "imageStorageDir", storageDirectory);

        assertThatThrownBy(() -> controller.uploadFile("undefined", pdf("certificat.pdf", "contenu")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private MockMultipartFile pdf(String name, String content) {
        return new MockMultipartFile("file", name, "application/pdf", content.getBytes());
    }
}
