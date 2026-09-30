package com.wild.corp.adhesion.shop.catalog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageStorageServiceTest {

    @TempDir Path storageDirectory;

    @Test
    void storesAllowedImageWithGeneratedName() {
        ProductImageStorageService storage = new ProductImageStorageService(storageDirectory);

        String fileName = storage.store(new MockMultipartFile("file", "product.png", "image/png", new byte[] {1, 2, 3}));

        assertThat(fileName).matches("[0-9a-f-]{36}\\.png");
        assertThat(storage.find(fileName).exists()).isTrue();
    }

    @Test
    void rejectsImageLargerThanOneMegabyte() {
        ProductImageStorageService storage = new ProductImageStorageService(storageDirectory);
        byte[] tooLarge = new byte[(int) ProductImageStorageService.MAX_FILE_SIZE_BYTES + 1];

        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "product.png", "image/png", tooLarge)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
