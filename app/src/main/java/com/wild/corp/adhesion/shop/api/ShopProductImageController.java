package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.catalog.service.ProductImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/shop/product-images")
public class ShopProductImageController {

    private final ProductImageStorageService imageStorage;

    public ShopProductImageController(ProductImageStorageService imageStorage) {
        this.imageStorage = imageStorage;
    }

    @GetMapping("/{fileName}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> image(@PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(mediaTypeFor(fileName))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(imageStorage.find(fileName));
    }

    private static MediaType mediaTypeFor(String fileName) {
        if (fileName.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (fileName.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (fileName.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
