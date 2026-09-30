package com.wild.corp.adhesion.shop.catalog.model;

import com.wild.corp.adhesion.shop.common.money.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductVariantStockTest {

    @Test
    void updatesPhysicalStockWithoutChangingCatalogDetailsOrReservations() {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        variant.trackStock(8);
        variant.reserveStock(3);

        variant.updateStockOnHand(5, 0);

        assertThat(variant.getStockOnHand()).isEqualTo(5);
        assertThat(variant.getStockReserved()).isEqualTo(3);
        assertThat(variant.availableStock()).isEqualTo(2);
        assertThat(variant.getSku()).isEqualTo("TS-M");
        assertThat(variant.getLabel()).isEqualTo("M");
        assertThat(variant.getPrice()).isEqualTo(new Money(1_500, "EUR"));
        assertThat(variant.isActive()).isTrue();

        assertThatThrownBy(() -> variant.updateStockOnHand(2, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(variant.getStockOnHand()).isEqualTo(5);

        assertThatThrownBy(() -> variant.updateStockOnHand(7, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Actualisez la page");
        assertThat(variant.getStockOnHand()).isEqualTo(5);
    }

    @Test
    void rejectsUpdatingUntrackedVariant() {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);

        assertThatThrownBy(() -> variant.updateStockOnHand(5, 0))
                .isInstanceOf(IllegalStateException.class);
    }
}
