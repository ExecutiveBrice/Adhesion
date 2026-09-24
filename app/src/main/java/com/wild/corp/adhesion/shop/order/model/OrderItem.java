package com.wild.corp.adhesion.shop.order.model;

import com.wild.corp.adhesion.shop.common.exception.InvalidQuantityException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Immutable commercial snapshot. No association to a mutable catalog entity is kept.
 */
@Entity
@Table(name = "shop_order_items")
public class OrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "variant_name", length = 160)
    private String variantName;

    @Column(length = 100)
    private String sku;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountInCents",
                    column = @Column(name = "unit_price_amount_cents", nullable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "unit_price_currency", nullable = false, length = 3))
    })
    private Money unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "stock_reserved", nullable = false)
    private boolean stockReserved;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountInCents",
                    column = @Column(name = "line_total_amount_cents", nullable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "line_total_currency", nullable = false, length = 3))
    })
    private Money lineTotal;

    protected OrderItem() {
        // Required by JPA.
    }

    public static OrderItem snapshot(Long productId,
                                     String productName,
                                     Long productVariantId,
                                     String variantName,
                                     String sku,
                                     Money unitPrice,
                                     int quantity) {
        return snapshot(productId, productName, productVariantId, variantName, sku, unitPrice, quantity, false);
    }

    public static OrderItem snapshot(Long productId,
                                     String productName,
                                     Long productVariantId,
                                     String variantName,
                                     String sku,
                                     Money unitPrice,
                                     int quantity,
                                     boolean stockReserved) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        OrderItem item = new OrderItem();
        item.productId = Objects.requireNonNull(productId, "L'identifiant du produit est obligatoire");
        item.productName = requireText(productName, "Le nom du produit est obligatoire");
        item.productVariantId = productVariantId;
        item.variantName = normalizeOptionalText(variantName);
        item.sku = normalizeOptionalText(sku);
        item.unitPrice = Objects.requireNonNull(unitPrice, "Le prix unitaire est obligatoire");
        item.quantity = quantity;
        item.stockReserved = stockReserved;
        item.lineTotal = unitPrice.multiply(quantity);
        return item;
    }

    void attachTo(ShopOrder order) {
        if (this.order != null && this.order != order) {
            throw new IllegalStateException("La ligne appartient déjà à une autre commande");
        }
        this.order = Objects.requireNonNull(order, "La commande est obligatoire");
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Long getProductVariantId() {
        return productVariantId;
    }

    public String getVariantName() {
        return variantName;
    }

    public String getSku() {
        return sku;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isStockReserved() {
        return stockReserved;
    }

    public Money getLineTotal() {
        return lineTotal;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
