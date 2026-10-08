package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.repository.PubliciteRepository;
import com.wild.corp.adhesion.repository.PubliciteRepository.Publicite;
import com.wild.corp.adhesion.repository.SectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;

@Service
public class PubliciteService {
    public static final long MAX_FILE_SIZE = 1024 * 1024;
    private final PubliciteRepository repository;
    private final SectionRepository sections;

    public PubliciteService(PubliciteRepository repository, SectionRepository sections) {
        this.repository = repository;
        this.sections = sections;
    }

    public List<Publicite> list(String username, boolean viewAll) {
        return viewAll ? repository.findAll() : repository.findVisible(username);
    }

    public PubliciteRepository.Image image(long id, String username, boolean viewAll) {
        return repository.findImage(id, username, viewAll)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Publicite create(String description, String facebookUrl, String instagramUrl, String websiteUrl,
                            List<Long> sectionIds, Integer displayOrder, boolean active, MultipartFile file) {
        String validDescription = validDescription(description);
        List<Long> audience = validSections(sectionIds);
        int order = validOrder(displayOrder);
        ValidatedImage image = validImage(file);
        return repository.create(validDescription, validUrl(facebookUrl, "Facebook"),
                validUrl(instagramUrl, "Instagram"), validUrl(websiteUrl, "site internet"),
                audience, order, active, image.contentType(), image.bytes());
    }

    public Publicite update(long id, String description, String facebookUrl, String instagramUrl, String websiteUrl,
                            List<Long> sectionIds, Integer displayOrder, boolean active, MultipartFile file) {
        String validDescription = validDescription(description);
        List<Long> audience = validSections(sectionIds);
        int order = validOrder(displayOrder);
        ValidatedImage image = file == null ? null : validImage(file);
        return repository.update(id, validDescription, validUrl(facebookUrl, "Facebook"),
                        validUrl(instagramUrl, "Instagram"), validUrl(websiteUrl, "site internet"), audience,
                        order, active, image == null ? null : image.contentType(), image == null ? null : image.bytes())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String validDescription(String description) {
        if (description == null || description.isBlank() || description.trim().length() > 200) {
            throw invalid("Renseignez une description de 1 à 200 caractères.");
        }
        return description.trim();
    }

    private record ValidatedImage(String contentType, byte[] bytes) {}

    private static ValidatedImage validImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw invalid("Choisissez une image PNG ou JPEG de 1 Mo maximum.");
        }
        try {
            byte[] bytes = file.getBytes();
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid("Le fichier doit être une image PNG ou JPEG valide.");
                var reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!List.of("png", "jpeg").contains(format)) {
                        throw invalid("Seules les images PNG et JPEG sont acceptées.");
                    }
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > 16_000_000) {
                        throw invalid("L’image ne doit pas dépasser 16 millions de pixels.");
                    }
                    if ((long) width * 9 != (long) height * 19) {
                        throw invalid("L’image doit respecter le format 19/9 (par exemple 1900 × 900 pixels).");
                    }
                    // Decode as well as inspecting the header to reject corrupt uploads.
                    reader.read(0);
                    return new ValidatedImage("image/" + format, bytes);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException exception) {
            throw invalid("L’image est illisible. Choisissez une image PNG ou JPEG valide.");
        }
    }

    public void delete(long id) {
        if (!repository.delete(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public Publicite updateSettings(long id, Integer displayOrder, Boolean active) {
        int order = validOrder(displayOrder);
        if (active == null) throw invalid("Indiquez si la publicité est active.");
        if (!repository.updateSettings(id, order, active)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String validUrl(String value, String label) {
        if (value == null || value.isBlank()) return null;
        String url = value.trim();
        if (url.length() > 2048) throw invalid("Le lien " + label + " est trop long.");
        try {
            URI uri = new URI(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw invalid("Le lien " + label + " doit être une adresse http ou https valide.");
            }
            return url;
        } catch (URISyntaxException exception) {
            throw invalid("Le lien " + label + " doit être une adresse http ou https valide.");
        }
    }

    private List<Long> validSections(List<Long> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return List.of();
        var unique = new LinkedHashSet<>(sectionIds);
        if (unique.size() > 100 || unique.stream().anyMatch(id -> id == null || id <= 0)
                || sections.findAllById(unique).size() != unique.size()) {
            throw invalid("Une ou plusieurs sections sélectionnées sont invalides.");
        }
        return List.copyOf(unique);
    }

    private static int validOrder(Integer displayOrder) {
        if (displayOrder == null || displayOrder < 0 || displayOrder > 10_000) {
            throw invalid("Le numéro d’ordre doit être compris entre 0 et 10 000.");
        }
        return displayOrder;
    }
}
