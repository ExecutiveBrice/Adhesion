package com.wild.corp.adhesion.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/files")
public class FileUploadController {


    @Value("${image-storage-dir}")
    private Path imageStorageDir;

    @PostMapping(value = "/", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> uploadFile(@RequestParam("adherentId") String adherentId,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        Path adherentDirectory = resolveAdherentDirectory(adherentId);
        Files.createDirectories(adherentDirectory);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return new ResponseEntity<>("Le nom du fichier est obligatoire", HttpStatus.BAD_REQUEST);
        }

        String fileName = Path.of(originalFilename).getFileName().toString();
        Path targetPath = adherentDirectory.resolve(fileName).normalize();
        try (var out = Files.newOutputStream(targetPath, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            file.getInputStream().transferTo(out);
        }

        return new ResponseEntity<>(fileName, HttpStatus.OK);
    }



    @RequestMapping(value = "/allFilesName", method = RequestMethod.GET)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<String>> allFilesName(@RequestParam("adherentId") String adherentId) throws IOException {
        Path path = resolveAdherentDirectory(adherentId);
        List<String> allFilesName = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path1 -> {
                     allFilesName.add(path1.getFileName().toString());
                 });
        }catch (IOException e){
            return new ResponseEntity<>(allFilesName, HttpStatus.OK);
        }


        return new ResponseEntity<>(allFilesName, HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> uploadFile(@RequestParam("adherentId") String adherentId,@RequestParam("fileName") String fileName) throws IOException {
        Path path = resolveFile(adherentId, fileName);
        String imageEncoded = Base64.getEncoder().encodeToString(Files.newInputStream(path).readAllBytes());

        return new ResponseEntity<>(imageEncoded, HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteFile(@RequestParam("adherentId") String adherentId,@RequestParam("fileName") String fileName) throws IOException {
        Path path = resolveFile(adherentId, fileName);

        try {
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NO_CONTENT);

        }
        return new ResponseEntity<>(fileName, HttpStatus.OK);
    }

    private Path resolveAdherentDirectory(String adherentId) {
        if (!adherentId.matches("\\d+")) {
            throw new IllegalArgumentException("Identifiant adhérent invalide");
        }
        return imageStorageDir.toAbsolutePath().normalize().resolve(adherentId);
    }

    private Path resolveFile(String adherentId, String fileName) {
        Path adherentDirectory = resolveAdherentDirectory(adherentId);
        Path file = adherentDirectory.resolve(fileName).normalize();
        if (!file.startsWith(adherentDirectory)) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        return file;
    }
}
