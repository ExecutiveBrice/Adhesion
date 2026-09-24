package com.wild.corp.adhesion.shop.catalog.model;

import com.wild.corp.adhesion.shop.common.exception.InsufficientStockException;
import com.wild.corp.adhesion.shop.common.exception.ProductNotOrderableException;
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
import jakarta.persistence.Version;

import java.util.Objects;

@Entity
@Table(name = "shop_product_variants")
public class ProductVariant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100, unique = true)
    private String sku;

    @Column(length = 160)
    private String label;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountInCents",
                    column = @Column(name = "price_amount_cents", nullable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "price_currency", nullable = false, length = 3))
    })
    private Money price;

    @Column(name = "stock_tracked", nullable = false)
    private boolean stockTracked;

    @Column(name = "stock_on_hand")
    private Long stockOnHand;

    @Column(name = "stock_reserved", nullable = false)
    private long stockReserved;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProductVariant() {
        // Required by JPA.
    }

    public ProductVariant(Product product, String sku, String label, Money price, boolean active, int displayOrder) {
        this.product = Objects.requireNonNull(product, "Le produit est obligatoire");
        this.sku = requireText(sku, "Le SKU est obligatoire");
        this.label = normalizeOptionalText(label);
        this.price = Objects.requireNonNull(price, "Le prix est obligatoire");
        this.active = active;
        setDisplayOrder(displayOrder);
        this.stockTracked = false;
        this.stockOnHand = null;
        this.stockReserved = 0;
    }

    public void changePrice(Money price) {
        this.price = Objects.requireNonNull(price, "Le prix est obligatoire");
    }

    public void trackStock(long quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif");
        }
        stockTracked = true;
        stockOnHand = quantity;
        stockReserved = 0;
    }

    public void stopTrackingStock() {
        if (stockReserved > 0) {
            throw new IllegalStateException("Le suivi du stock ne peut pas être arrêté pendant une réservation");
        }
        stockTracked = false;
        stockOnHand = null;
    }

    public void ensureOrderable(int requestedQuantity) {
        if (!active || !product.isActive()) {
            throw new ProductNotOrderableException(product.getId());
        }
        if (stockTracked && availableStock() < requestedQuantity) {
            throw new InsufficientStockException(id, availableStock(), requestedQuantity);
        }
    }

    public boolean reserveStock(int requestedQuantity) {
        ensureOrderable(requestedQuantity);
        if (!stockTracked) {
            return false;
        }
        stockReserved = Math.addExact(stockReserved, requestedQuantity);
        return true;
    }

    public void releaseStock(int quantity) {
        requireExistingReservation(quantity);
        stockReserved -= quantity;
    }

    public void consumeReservedStock(int quantity) {
        requireExistingReservation(quantity);
        stockReserved -= quantity;
        stockOnHand -= quantity;
    }

    public long availableStock() {
        return stockTracked && stockOnHand != null ? stockOnHand - stockReserved : Long.MAX_VALUE;
    }

    private void requireExistingReservation(int quantity) {
        if (quantity <= 0 || !stockTracked || stockOnHand == null || stockReserved < quantity) {
            throw new IllegalStateException("La réservation de stock est absente ou insuffisante");
        }
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void updateDetails(String sku, String label, Money price, boolean active, int displayOrder,
                              boolean stockTracked, Long stockOnHand) {
        this.sku = requireText(sku, "Le SKU est obligatoire");
        this.label = normalizeOptionalText(label);
        this.price = Objects.requireNonNull(price, "Le prix est obligatoire");
        this.active = active;
        setDisplayOrder(displayOrder);
        if (!stockTracked) {
            stopTrackingStock();
            return;
        }
        if (stockOnHand == null || stockOnHand < stockReserved) {
            throw new IllegalArgumentException("Le stock doit couvrir les réservations en cours");
        }
        this.stockTracked = true;
        this.stockOnHand = stockOnHand;
    }

    public void setDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("L'ordre d'affichage ne peut pas être négatif");
        }
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getSku() {
        return sku;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Money getPrice() {
        return price;
    }

    public boolean isStockTracked() {
        return stockTracked;
    }

    public Long getStockOnHand() {
        return stockOnHand;
    }

    public long getStockReserved() {
        return stockReserved;
    }

    public long getVersion() {
        return version;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ProductVariant variant)) {
            return false;
        }
        return id != null && Objects.equals(id, variant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
