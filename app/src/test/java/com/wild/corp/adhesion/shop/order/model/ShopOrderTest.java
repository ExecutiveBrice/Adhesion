package com.wild.corp.adhesion.shop.order.model;

import com.wild.corp.adhesion.shop.common.exception.InvalidQuantityException;
import com.wild.corp.adhesion.shop.common.exception.InvalidStatusTransitionException;
import com.wild.corp.adhesion.shop.common.money.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopOrderTest {

    @Test
    void calculatesOrderTotalFromServerSideLineSnapshots() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");

        order.addItem(OrderItem.snapshot(1L, "Tee-shirt", 11L, "M", "TS-M",
                new Money(1_250, "EUR"), 2));
        order.addItem(OrderItem.snapshot(2L, "Badge", 21L, null, "BADGE",
                new Money(400, "EUR"), 3));

        assertThat(order.getTotal()).isEqualTo(new Money(3_700, "EUR"));
        assertThat(order.getItems()).extracting(OrderItem::getLineTotal)
                .containsExactly(new Money(2_500, "EUR"), new Money(1_200, "EUR"));
    }

    @Test
    void rejectsInvalidQuantity() {
        assertThatThrownBy(() -> OrderItem.snapshot(1L, "Tee-shirt", 11L, "M", "TS-M",
                new Money(1_250, "EUR"), 0))
                .isInstanceOf(InvalidQuantityException.class)
                .hasMessageContaining("strictement positive");
    }

    @Test
    void rejectsForbiddenStatusTransition() {
        ShopOrder order = new ShopOrder("CMD-2026-000002", 42L, "EUR");

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.COMPLETED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("DRAFT -> COMPLETED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }
}
