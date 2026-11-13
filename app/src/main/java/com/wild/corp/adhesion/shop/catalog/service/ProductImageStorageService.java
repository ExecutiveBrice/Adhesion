package com.wild.corp.adhesion.shop.catalog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Stores product visuals outside the database, in the application image volume. */
@Service
public class ProductImageStorageService {

    public static final long MAX_FILE_SIZE_BYTES = 1_024 * 1_024;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    private final Path productImagesDirectory;

    public ProductImageStorageService(@Value("${image-storage-dir}") Path imageStorageDirectory) {
        this.productImagesDirectory = imageStorageDirectory.toAbsolutePath().normalize().resolve("shop-products");
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Choisissez une image à importer.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw badRequest("L'image ne doit pas dépasser 1 Mo.");
        }

        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw badRequest("Le fichier doit être une image PNG, JPEG, GIF ou WebP.");
        }

        try {
            Files.createDirectories(productImagesDirectory);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = productImagesDirectory.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "L'image n'a pas pu être enregistrée.", exception);
        }
    }

    public Resource find(String fileName) {
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif|webp)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path image = productImagesDirectory.resolve(fileName).normalize();
        if (!image.startsWith(productImagesDirectory) || !Files.isRegularFile(image)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(image);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
