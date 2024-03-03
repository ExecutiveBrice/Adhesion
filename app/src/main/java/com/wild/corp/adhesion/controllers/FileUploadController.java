package com.wild.corp.adhesion.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    @RequestMapping(value = "/", method = RequestMethod.POST)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> uploadFile(@RequestParam("adherentId") String adherentId,@RequestParam("fileName") String fileName, @RequestBody String fileContent) throws IOException {

        byte[] imageDecoded = Base64.getDecoder().decode(fileContent.getBytes());
        Path prePath = this.imageStorageDir.resolve(adherentId);

        if (!Files.exists(prePath)) {
            Files.createDirectories(prePath);
        }

        final Path targetPath = prePath.resolve(fileName);
        try (InputStream in = new ByteArrayInputStream(imageDecoded)) {
            try (OutputStream out = Files.newOutputStream(targetPath, StandardOpenOption.CREATE)) {
                in.transferTo(out);
            }
        }

        return new ResponseEntity<>(fileContent, HttpStatus.OK);
    }



    @RequestMapping(value = "/allFilesName", method = RequestMethod.GET)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<String>> allFilesName(@RequestParam("adherentId") String adherentId) throws IOException {
        Path path =this.imageStorageDir.resolve(adherentId);
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
        Path path =this.imageStorageDir.resolve(adherentId+"/"+fileName);
        String imageEncoded = Base64.getEncoder().encodeToString(Files.newInputStream(path).readAllBytes());

        return new ResponseEntity<>(imageEncoded, HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteFile(@RequestParam("adherentId") String adherentId,@RequestParam("fileName") String fileName) throws IOException {
        Path path =this.imageStorageDir.resolve(adherentId+"/"+fileName);

        try {
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NO_CONTENT);

        }
        return new ResponseEntity<>(fileName, HttpStatus.OK);
    }




}