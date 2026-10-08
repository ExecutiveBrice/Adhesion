package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.Section;
import com.wild.corp.adhesion.repository.PubliciteRepository;
import com.wild.corp.adhesion.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PubliciteServiceTest {
    private final PubliciteRepository repository = mock(PubliciteRepository.class);
    private final SectionRepository sections = mock(SectionRepository.class);
    private final PubliciteService service = new PubliciteService(repository, sections);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void configureRepository() {
        when(repository.create(anyString(), nullable(String.class), nullable(String.class), nullable(String.class),
                anyList(), anyInt(), anyBoolean(), anyString(), any())).thenAnswer(call -> new PubliciteRepository.Publicite(1L,
                call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3), call.getArgument(4),
                call.getArgument(5), call.getArgument(6)));
        when(sections.findAllById(any())).thenAnswer(call -> {
            var result = new ArrayList<Section>();
            ((Iterable<Long>) call.getArgument(0)).forEach(id -> result.add(Section.builder()
                    .id(id).nom("Section " + id).type("NON_COMPETITIVE").build()));
            return result;
        });
    }

    @Test
    void acceptsNineteenByNineLinksAndSections() throws Exception {
        var file = image(190, 90, "png");
        var result = service.create("  Fête de l’association  ", " https://facebook.com/alod ",
                "https://instagram.com/alod", "https://alod.fr", List.of(3L, 9L, 3L), 12, true, file);
        assertThat(result.description()).isEqualTo("Fête de l’association");
        assertThat(result.facebookUrl()).isEqualTo("https://facebook.com/alod");
        assertThat(result.sectionIds()).containsExactly(3L, 9L);
        assertThat(result.displayOrder()).isEqualTo(12);
        assertThat(result.active()).isTrue();
        verify(repository).create(eq("Fête de l’association"), eq("https://facebook.com/alod"),
                eq("https://instagram.com/alod"), eq("https://alod.fr"), eq(List.of(3L, 9L)),
                eq(12), eq(true), eq("image/png"), eq(file.getBytes()));
        service.create("JPEG", null, "", null, null, 0, false, image(380, 180, "jpeg"));
        verify(repository).create(eq("JPEG"), isNull(), isNull(), isNull(), eq(List.of()), eq(0), eq(false),
                eq("image/jpeg"), any());
    }

    @Test
    void delegatesAudienceFilteringAndImageAuthorization() {
        service.list("member@alod.fr", false);
        verify(repository).findVisible("member@alod.fr");
        service.list("communication@alod.fr", true);
        verify(repository).findAll();

        when(repository.findImage(7, "member@alod.fr", false))
                .thenReturn(Optional.of(new PubliciteRepository.Image("image/png", new byte[]{1})));
        assertThat(service.image(7, "member@alod.fr", false).bytes()).containsExactly(1);
    }

    @Test
    void rejectsUnknownSections() throws Exception {
        doReturn(List.of(Section.builder().id(3L).nom("Une").type("NON_COMPETITIVE").build()))
                .when(sections).findAllById(any());
        assertThatThrownBy(() -> service.create("Affiche", null, null, null, List.of(3L, 99L), 0, true, image(190, 90, "png")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("sections");
        verify(repository, never()).create(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any(), any());
    }

    @Test
    void rejectsWrongRatioEvenWhenTheMimeTypeClaimsPng() throws Exception {
        assertThatThrownBy(() -> service.create("Affiche", null, null, null, List.of(), 0, true, image(160, 90, "png")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("19/9");
        verify(repository, never()).create(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any(), any());
    }

    @Test
    void rejectsNonImagesEmptyOversizedAndUnsupportedFiles() throws Exception {
        var fake = new MockMultipartFile("file", "affiche.png", "image/png", "<svg/>".getBytes());
        var empty = new MockMultipartFile("file", new byte[0]);
        var oversized = new MockMultipartFile("file", new byte[(int) PubliciteService.MAX_FILE_SIZE + 1]);
        for (var file : new MockMultipartFile[]{fake, empty, oversized, image(190, 90, "gif")}) {
            assertThatThrownBy(() -> service.create("Affiche", null, null, null, List.of(), 0, true, file))
                    .isInstanceOf(ResponseStatusException.class);
        }
        verify(repository, never()).create(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any(), any());
    }

    @Test
    void rejectsInvalidDescriptionAndExcessiveDimensions() throws Exception {
        var file = image(190, 90, "png");
        assertThatThrownBy(() -> service.create(" ", null, null, null, List.of(), 0, true, file)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create("a".repeat(201), null, null, null, List.of(), 0, true, file)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create("Affiche", null, null, null, List.of(), 0, true, image(7600, 3600, "png")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("pixels");
        verify(repository, never()).create(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any(), any());
    }

    @Test
    void rejectsUnsafeOrMalformedLinks() throws Exception {
        var file = image(190, 90, "png");
        for (String url : new String[]{"javascript:alert(1)", "facebook.com/alod", "https://"}) {
            assertThatThrownBy(() -> service.create("Affiche", url, null, null, List.of(), 0, true, file))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Facebook");
        }
        verify(repository, never()).create(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any(), any());
    }

    @Test
    void updatesAndValidatesOrderAndActiveState() {
        var updated = new PubliciteRepository.Publicite(7L, "Affiche", null, null, null, List.of(), 3, false);
        when(repository.updateSettings(7, 3, false)).thenReturn(true);
        when(repository.findById(7)).thenReturn(Optional.of(updated));
        assertThat(service.updateSettings(7, 3, false)).isEqualTo(updated);

        for (Integer order : new Integer[]{null, -1, 10_001}) {
            assertThatThrownBy(() -> service.updateSettings(7, order, true))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ordre");
        }
        assertThatThrownBy(() -> service.updateSettings(7, 1, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("active");
    }

    @Test
    void editsAllFieldsAndKeepsTheImageWhenNoneIsProvided() {
        var updated = new PubliciteRepository.Publicite(7L, "Affiche modifiée", null, null,
                "https://alod.fr", List.of(3L), 4, false);
        when(repository.update(7, "Affiche modifiée", null, null, "https://alod.fr", List.of(3L),
                4, false, null, null)).thenReturn(Optional.of(updated));

        assertThat(service.update(7, " Affiche modifiée ", null, null, "https://alod.fr", List.of(3L),
                4, false, null)).isEqualTo(updated);
    }

    @Test
    void validatesAndReplacesTheImageDuringEdition() throws Exception {
        var file = image(190, 90, "png");
        var updated = new PubliciteRepository.Publicite(7L, "Affiche", null, null, null, List.of(), 1, true);
        when(repository.update(eq(7L), eq("Affiche"), isNull(), isNull(), isNull(), eq(List.of()), eq(1),
                eq(true), eq("image/png"), any())).thenReturn(Optional.of(updated));

        assertThat(service.update(7, "Affiche", null, null, null, List.of(), 1, true, file)).isEqualTo(updated);
        verify(repository).update(eq(7L), eq("Affiche"), isNull(), isNull(), isNull(), eq(List.of()), eq(1),
                eq(true), eq("image/png"), eq(file.getBytes()));
    }

    @Test
    void returnsNotFoundForMissingImagesAndDeletes() {
        when(repository.findImage(42, "member", false)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.image(42, "member", false)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThatThrownBy(() -> service.delete(42)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }

    private MockMultipartFile image(int width, int height, String format) throws Exception {
        var buffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, buffer);
        return new MockMultipartFile("file", "affiche.dat", "application/octet-stream", buffer.toByteArray());
    }
}
