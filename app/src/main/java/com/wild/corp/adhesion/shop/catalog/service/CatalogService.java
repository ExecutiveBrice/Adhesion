package com.wild.corp.adhesion.shop.catalog.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductCategory;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductCategoryRepository;
import com.wild.corp.adhesion.shop.catalog.repository.ProductRepository;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class CatalogService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;

    public CatalogService(ProductRepository productRepository,
                          ProductCategoryRepository categoryRepository,
                          ProductVariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public ProductCategory saveCategory(ProductCategory category) {
        return categoryRepository.save(category);
    }

    public Product changeVariantPrice(Long variantId, Money newPrice) {
        ProductVariant variant = variantRepository.findById(variantId).orElseThrow();
        variant.changePrice(newPrice);
        return variant.getProduct();
    }

    public Product setProductActive(Long productId, boolean active) {
        Product product = productRepository.findById(productId).orElseThrow();
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
        return product;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Product> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Product findActiveProduct(Long productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new NoSuchElementException("Produit introuvable"));
    }
}
