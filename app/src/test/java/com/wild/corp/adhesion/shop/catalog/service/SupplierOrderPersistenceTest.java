package com.wild.corp.adhesion.shop.catalog.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrderStatus;
import com.wild.corp.adhesion.shop.catalog.repository.ProductRepository;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.catalog.repository.SupplierOrderRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = SupplierOrderPersistenceTest.JpaApplication.class)
class SupplierOrderPersistenceTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.wild.corp.adhesion.shop")
    @EnableJpaRepositories("com.wild.corp.adhesion.shop")
    @Import(SupplierOrderService.class)
    static class JpaApplication { }

    @Autowired private ProductRepository products;
    @Autowired private ProductVariantRepository variants;
    @Autowired private SupplierOrderRepository supplierOrders;
    @Autowired private SupplierOrderService service;
    @Autowired private EntityManager entityManager;

    @Test
    void draftSurvivesReloadAndCanReplaceSameVariantBeforePlacement() {
        Product product = products.save(new Product("Tee-shirt", "tee-shirt", null, true, 0));
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        variant.trackStock(0);
        variants.saveAndFlush(variant);
        variant.updateStockOnHand(-2, variant.getVersion());
        variants.flush();

        Long id = service.createDraft("Fournisseur A", null,
                List.of(new SupplierOrderService.Line(variant.getId(), 2))).getId();
        supplierOrders.flush();
        entityManager.clear();

        var reloaded = supplierOrders.findAllByOrderByCreatedAtDescIdDesc().getFirst();
        assertThat(reloaded.getStatus()).isEqualTo(SupplierOrderStatus.DRAFT);
        assertThat(reloaded.getLines().getFirst().getLineTotalAmountInCents()).isNull();

        service.updateDraft(id, "Fournisseur A", "BC-1",
                List.of(new SupplierOrderService.Line(variant.getId(), 2)));
        supplierOrders.flush();
        entityManager.clear();

        service.placeDraft(id);
        supplierOrders.flush();
        entityManager.clear();
        var placed = supplierOrders.findAllByOrderByCreatedAtDescIdDesc().getFirst();
        assertThat(placed.getStatus()).isEqualTo(SupplierOrderStatus.ORDERED);
        assertThat(placed.getLines()).hasSize(1);
        assertThat(placed.getLines().getFirst().getLineTotalAmountInCents()).isNull();
    }
}
