package com.wild.corp.adhesion.services;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUtilsTest {

    @Test
    void compressesAndDecompressesWithoutLosingData() {
        byte[] original = "image-content".repeat(1_000).getBytes(StandardCharsets.UTF_8);

        byte[] compressed = ImageUtils.compressImage(original);

        assertThat(compressed).isNotEqualTo(original).hasSizeLessThan(original.length);
        assertThat(ImageUtils.decompressImage(compressed)).isEqualTo(original);
    }

    @Test
    void rejectsInvalidCompressedData() {
        assertThatThrownBy(() -> ImageUtils.decompressImage(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalides");
    }
}
