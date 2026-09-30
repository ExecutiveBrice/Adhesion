package com.wild.corp.adhesion.shop.order.model;

import com.wild.corp.adhesion.shop.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shop_order_messages")
public class OrderMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 32)
    private OrderMessageSender senderType;

    @Column(nullable = false, length = 2000)
    private String content;

    protected OrderMessage() {
        // Required by JPA.
    }

    public OrderMessage(ShopOrder order, Long senderUserId, OrderMessageSender senderType, String content) {
        if (order == null || senderType == null) {
            throw new IllegalArgumentException("La commande et l'expéditeur sont obligatoires");
        }
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty() || normalizedContent.length() > 2000) {
            throw new IllegalArgumentException("Le message doit contenir entre 1 et 2000 caractères");
        }
        this.order = order;
        this.senderUserId = senderUserId;
        this.senderType = senderType;
        this.content = normalizedContent;
    }

    public Long getId() { return id; }
    public OrderMessageSender getSenderType() { return senderType; }
    public String getContent() { return content; }
}
