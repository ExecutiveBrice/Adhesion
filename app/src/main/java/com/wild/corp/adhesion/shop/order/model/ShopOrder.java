package com.wild.corp.adhesion.shop.order.model;

import com.wild.corp.adhesion.shop.common.exception.InvalidStatusTransitionException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "shop_orders")
public class ShopOrder extends AuditableEntity {

    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.DRAFT, EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED),
            OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.EXPIRED),
            OrderStatus.PAID, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.REFUNDED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED),
            OrderStatus.COMPLETED, EnumSet.of(OrderStatus.REFUNDED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.EXPIRED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 40, unique = true)
    private String orderNumber;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amountInCents",
                    column = @Column(name = "total_amount_cents", nullable = false)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "total_currency", nullable = false, length = 3))
    })
    private Money total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected ShopOrder() {
        // Required by JPA.
    }

    public ShopOrder(String orderNumber, Long customerUserId, String currency) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Le numéro de commande est obligatoire");
        }
        if (customerUserId == null) {
            throw new IllegalArgumentException("Le client est obligatoire");
        }
        this.orderNumber = orderNumber.trim();
        this.customerUserId = customerUserId;
        this.status = OrderStatus.DRAFT;
        this.total = Money.zero(currency);
    }

    public void addItem(OrderItem item) {
        requireDraft();
        item.getLineTotal().requireSameCurrency(total);
        item.attachTo(this);
        items.add(item);
        recalculateTotal();
    }

    public void submitForPayment() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Une commande vide ne peut pas être soumise au paiement");
        }
        transitionTo(OrderStatus.PENDING_PAYMENT);
    }

    public void transitionTo(OrderStatus targetStatus) {
        if (targetStatus == null || !ALLOWED_TRANSITIONS.get(status).contains(targetStatus)) {
            throw new InvalidStatusTransitionException(status, targetStatus);
        }
        status = targetStatus;
    }

    private void recalculateTotal() {
        total = OrderTotalsCalculator.calculate(items, total.getCurrency());
    }

    private void requireDraft() {
        if (status != OrderStatus.DRAFT) {
            throw new InvalidStatusTransitionException(status, OrderStatus.DRAFT);
        }
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotal() {
        return total;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public long getVersion() {
        return version;
    }
}
