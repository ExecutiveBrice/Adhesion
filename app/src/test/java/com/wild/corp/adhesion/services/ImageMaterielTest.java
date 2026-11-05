package com.wild.corp.adhesion.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class ImageMaterielTest {
    static String photo(int largeur, int hauteur) throws Exception {
        var sortie = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_ARGB), "png", sortie);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(sortie.toByteArray());
    }

    @Test
    void fitsLandscapePortraitAndSmallPhotosWithoutStretchingOrEnlarging() throws Exception {
        for (int[] dimensions : new int[][] {{1600, 900, 640, 360}, {900, 1600, 270, 480}, {120, 80, 120, 80}}) {
            String resultat = ImageMateriel.redimensionner(photo(dimensions[0], dimensions[1]));
            assertThat(resultat).startsWith("data:image/jpeg;base64,");
            var image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(resultat.split(",")[1])));
            assertThat(image.getWidth()).isEqualTo(dimensions[2]);
            assertThat(image.getHeight()).isEqualTo(dimensions[3]);
            assertThat(image.getRGB(0, 0) & 0xffffff).isEqualTo(0xffffff);
        }
    }

    @Test
    void handlesMissingPhotosAndRejectsInvalidOrExcessiveImages() {
        assertThat(ImageMateriel.redimensionner(null)).isNull();
        assertThat(ImageMateriel.redimensionner("")).isNull();
        for (String image : new String[] {"https://example.org/image.jpg", "data:image/svg+xml;base64,PHN2Zz4=",
                "data:image/png;base64,???", "data:image/jpeg;base64,Ym9uam91cg==",
                "data:image/png;base64," + "A".repeat(7_000_000)}) {
            assertThatThrownBy(() -> ImageMateriel.redimensionner(image))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }
}
