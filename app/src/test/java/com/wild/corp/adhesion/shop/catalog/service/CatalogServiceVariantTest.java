package com.wild.corp.adhesion.shop.catalog.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductCategoryRepository;
import com.wild.corp.adhesion.shop.catalog.repository.ProductRepository;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogServiceVariantTest {

    @Test
    void createsVariantWithTrackedStockAtZero() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        when(products.findById(12L)).thenReturn(Optional.of(product));
        CatalogService catalog = new CatalogService(products, mock(ProductCategoryRepository.class),
                mock(ProductVariantRepository.class));

        ProductVariant variant = catalog.createVariant(12L, "TS-M", "M", new Money(1_500, "EUR"), true, 0);

        assertThat(variant.isStockTracked()).isTrue();
        assertThat(variant.getStockOnHand()).isZero();
        assertThat(variant.getStockReserved()).isZero();
        assertThat(product.getVariants()).contains(variant);
    }
}
