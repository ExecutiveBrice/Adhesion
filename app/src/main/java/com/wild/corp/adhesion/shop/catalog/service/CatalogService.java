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
import java.util.Set;

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

    public Product createProduct(String name, String slug, String description, String imageUrl, boolean active,
                                 int displayOrder, Set<Long> categoryIds) {
        Product product = new Product(name, slug, description, active, displayOrder);
        product.updateDetails(name, slug, description, imageUrl, displayOrder);
        product.replaceCategories(categories(categoryIds));
        return productRepository.save(product);
    }

    public Product updateProduct(Long productId, String name, String slug, String description, String imageUrl,
                                 boolean active, int displayOrder, Set<Long> categoryIds) {
        Product product = findProduct(productId);
        product.updateDetails(name, slug, description, imageUrl, displayOrder);
        if (active) product.activate(); else product.deactivate();
        product.replaceCategories(categories(categoryIds));
        return product;
    }

    public void deleteProduct(Long productId) {
        productRepository.delete(findProduct(productId));
    }

    public ProductVariant createVariant(Long productId, String sku, String label, Money price, boolean active,
                                        int displayOrder, boolean stockTracked, Long stockOnHand) {
        Product product = findProduct(productId);
        ProductVariant variant = new ProductVariant(product, sku, label, price, active, displayOrder);
        variant.updateDetails(sku, label, price, active, displayOrder, stockTracked, stockOnHand);
        product.addVariant(variant);
        return variant;
    }

    public ProductVariant updateVariant(Long variantId, String sku, String label, Money price, boolean active,
                                        int displayOrder, boolean stockTracked, Long stockOnHand) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("Variante introuvable"));
        variant.updateDetails(sku, label, price, active, displayOrder, stockTracked, stockOnHand);
        return variant;
    }

    public void deleteVariant(Long variantId) {
        variantRepository.deleteById(variantId);
    }

    public ProductCategory createCategory(String name, String slug, String description, boolean active, int displayOrder) {
        return categoryRepository.save(new ProductCategory(name, slug, description, active, displayOrder));
    }

    public ProductCategory updateCategory(Long categoryId, String name, String slug, String description,
                                          boolean active, int displayOrder) {
        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NoSuchElementException("Catégorie introuvable"));
        category.updateDetails(name, slug, description, active, displayOrder);
        return category;
    }

    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Product> findAllProducts() {
        return productRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProductCategory> findAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
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

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Produit introuvable"));
    }

    private List<ProductCategory> categories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return List.of();
        List<ProductCategory> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new NoSuchElementException("Une catégorie est introuvable");
        }
        return categories;
    }
}
