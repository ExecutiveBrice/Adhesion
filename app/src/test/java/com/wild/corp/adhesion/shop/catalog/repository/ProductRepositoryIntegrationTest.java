package com.wild.corp.adhesion.shop.catalog.repository;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductCategory;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.common.money.Money;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = ProductRepositoryIntegrationTest.JpaApplication.class)
class ProductRepositoryIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.wild.corp.adhesion.shop")
    @EnableJpaRepositories("com.wild.corp.adhesion.shop")
    static class JpaApplication { }

    @Autowired private ProductRepository products;
    @Autowired private ProductCategoryRepository categories;
    @Autowired private EntityManager entityManager;

    @Test
    void persistsOrderedAdditionalImagesAndTheirRemoval() {
        Product product = new Product("Photos", "photos", null, true, 0);
        product.updateDetails("Photos", "photos", null, "/main.jpg", 0);
        product.replaceAdditionalImages(List.of("/front.jpg", "/detail.jpg"));
        ProductVariant variant = new ProductVariant(product, "PHOTO-M", "M", new Money(1500, "EUR"), true, 0);
        variant.setImageUrl("/variant.jpg");
        product.addVariant(variant);
        products.saveAndFlush(product);
        entityManager.clear();

        Product reloaded = products.findByIdAndActiveTrue(product.getId()).orElseThrow();
        assertThat(reloaded.getImageUrl()).isEqualTo("/main.jpg");
        assertThat(reloaded.getAdditionalImageUrls()).containsExactly("/front.jpg", "/detail.jpg");
        assertThat(reloaded.getVariants().iterator().next().getImageUrl()).isEqualTo("/variant.jpg");
        reloaded.getVariants().iterator().next().setImageUrl(null);
        reloaded.replaceAdditionalImages(List.of("/detail.jpg"));
        products.flush();
        entityManager.clear();
        assertThat(products.findById(product.getId()).orElseThrow().getAdditionalImageUrls()).containsExactly("/detail.jpg");
        assertThat(products.findById(product.getId()).orElseThrow().getVariants().iterator().next().getImageUrl()).isNull();
    }

    @Test
    void loadsEachVariantOnlyOnceWhenTheProductHasMultipleCategories() {
        ProductCategory firstCategory = categories.save(new ProductCategory("Hauts", "hauts", null, true, 0));
        ProductCategory secondCategory = categories.save(new ProductCategory("Nouveautés", "nouveautes", null, true, 1));
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        product.addCategory(firstCategory);
        product.addCategory(secondCategory);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        product.addVariant(variant);
        products.saveAndFlush(product);
        entityManager.clear();

        Product reloaded = products.findAllByOrderByDisplayOrderAscNameAsc().getFirst();

        assertThat(reloaded.getCategories()).hasSize(2);
        assertThat(reloaded.getVariants()).containsExactly(variant);
    }
}
