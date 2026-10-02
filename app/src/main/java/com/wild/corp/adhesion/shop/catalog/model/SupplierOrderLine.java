package com.wild.corp.adhesion.shop.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "shop_supplier_order_lines")
public class SupplierOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private SupplierOrder order;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", length = 160)
    private String variantName;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_cost_amount_cents")
    private Long unitCostAmountInCents;

    @Column(name = "unit_cost_currency", length = 3)
    private String unitCostCurrency;

    @Column(name = "expected_need")
    private Integer expectedNeed;

    @Column(name = "extra_approved", nullable = false)
    private boolean extraApproved;

    protected SupplierOrderLine() { }

    SupplierOrderLine(SupplierOrder order, ProductVariant variant, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité commandée doit être positive");
        }
        this.order = Objects.requireNonNull(order);
        this.variantId = Objects.requireNonNull(variant.getId(), "La variante doit être enregistrée");
        this.productName = variant.getProduct().getName();
        this.variantName = variant.getLabel();
        this.sku = variant.getSku();
        this.quantity = quantity;
    }

    public void setPurchaseDetails(Long unitCostAmountInCents, String currency, Integer expectedNeed,
                                   boolean extraApproved) {
        if (unitCostAmountInCents != null && unitCostAmountInCents < 0) {
            throw new IllegalArgumentException("Le prix d'achat ne peut pas être négatif");
        }
        if (unitCostAmountInCents != null && !"EUR".equals(currency)) {
            throw new IllegalArgumentException("Le prix d'achat doit être en euros");
        }
        if (expectedNeed != null && expectedNeed < 0) {
            throw new IllegalArgumentException("Le besoin attendu ne peut pas être négatif");
        }
        this.unitCostAmountInCents = unitCostAmountInCents;
        this.unitCostCurrency = unitCostAmountInCents == null ? null : currency;
        this.expectedNeed = expectedNeed;
        this.extraApproved = extraApproved;
    }

    public Long getId() { return id; }
    public Long getVariantId() { return variantId; }
    public String getProductName() { return productName; }
    public String getVariantName() { return variantName; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public Long getUnitCostAmountInCents() { return unitCostAmountInCents; }
    public String getUnitCostCurrency() { return unitCostCurrency; }
    public Integer getExpectedNeed() { return expectedNeed; }
    public boolean isExtraApproved() { return extraApproved; }
    public Long getLineTotalAmountInCents() {
        return unitCostAmountInCents == null ? null : Math.multiplyExact(unitCostAmountInCents, quantity);
    }
}
