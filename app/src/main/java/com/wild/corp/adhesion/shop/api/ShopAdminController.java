package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.api.dto.AdminCategoryRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminCategoryResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminProductRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminProductResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantRequest;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantResponse;
import com.wild.corp.adhesion.shop.catalog.service.CatalogService;
import com.wild.corp.adhesion.shop.common.money.Money;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/shop/admin")
@PreAuthorize("hasRole('RESPONSABLE_BOUTIQUE')")
@Slf4j
public class ShopAdminController {

    private final CatalogService catalogService;

    public ShopAdminController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public List<AdminProductResponse> products() {
        List<AdminProductResponse> products = catalogService.findAllProducts().stream().map(ShopAdminMapper::product).toList();
        log.info("Gestion boutique : {} produit(s) chargé(s)", products.size());
        return products;
    }

    @PostMapping("/products")
    public ResponseEntity<AdminProductResponse> createProduct(@Valid @RequestBody AdminProductRequest request) {
        var product = catalogService.createProduct(request.name(), request.slug(), request.description(), request.imageUrl(),
                request.active(), request.displayOrder(), request.categoryIds());
        return ResponseEntity.created(URI.create("/shop/admin/products/" + product.getId()))
                .body(ShopAdminMapper.product(product));
    }

    @PutMapping("/products/{productId}")
    public AdminProductResponse updateProduct(@PathVariable Long productId, @Valid @RequestBody AdminProductRequest request) {
        return ShopAdminMapper.product(catalogService.updateProduct(productId, request.name(), request.slug(),
                request.description(), request.imageUrl(), request.active(), request.displayOrder(), request.categoryIds()));
    }

    @DeleteMapping("/products/{productId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        catalogService.deleteProduct(productId);
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<AdminVariantResponse> createVariant(@PathVariable Long productId,
                                                                @Valid @RequestBody AdminVariantRequest request) {
        var variant = catalogService.createVariant(productId, request.sku(), request.label(), money(request), request.active(),
                request.displayOrder(), request.stockTracked(), request.stockOnHand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopAdminMapper.variant(variant));
    }

    @PutMapping("/variants/{variantId}")
    public AdminVariantResponse updateVariant(@PathVariable Long variantId, @Valid @RequestBody AdminVariantRequest request) {
        return ShopAdminMapper.variant(catalogService.updateVariant(variantId, request.sku(), request.label(), money(request),
                request.active(), request.displayOrder(), request.stockTracked(), request.stockOnHand()));
    }

    @DeleteMapping("/variants/{variantId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@PathVariable Long variantId) {
        catalogService.deleteVariant(variantId);
    }

    @GetMapping("/categories")
    public List<AdminCategoryResponse> categories() {
        List<AdminCategoryResponse> categories = catalogService.findAllCategories().stream().map(ShopAdminMapper::category).toList();
        log.info("Gestion boutique : {} catégorie(s) chargée(s)", categories.size());
        return categories;
    }

    @PostMapping("/categories")
    public ResponseEntity<AdminCategoryResponse> createCategory(@Valid @RequestBody AdminCategoryRequest request) {
        var category = catalogService.createCategory(request.name(), request.slug(), request.description(), request.active(), request.displayOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopAdminMapper.category(category));
    }

    @PutMapping("/categories/{categoryId}")
    public AdminCategoryResponse updateCategory(@PathVariable Long categoryId, @Valid @RequestBody AdminCategoryRequest request) {
        return ShopAdminMapper.category(catalogService.updateCategory(categoryId, request.name(), request.slug(),
                request.description(), request.active(), request.displayOrder()));
    }

    @DeleteMapping("/categories/{categoryId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        catalogService.deleteCategory(categoryId);
    }

    private static Money money(AdminVariantRequest request) {
        return new Money(request.priceAmountInCents(), request.currency().toUpperCase());
    }
}
