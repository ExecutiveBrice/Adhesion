package com.wild.corp.adhesion.shop.order.model;

import com.wild.corp.adhesion.shop.common.money.Money;

import java.util.Collection;

public final class OrderTotalsCalculator {

    private OrderTotalsCalculator() {
    }

    public static Money calculate(Collection<OrderItem> items, String currency) {
        Money total = Money.zero(currency);
        for (OrderItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }
}
