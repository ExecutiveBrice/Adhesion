package com.wild.corp.adhesion.shop.order.model;

import com.wild.corp.adhesion.shop.common.money.Money;

import java.util.Collection;

public final class OrderTotalsCalculator {

    private OrderTotalsCalculator() {
    }

    public static Money calculate(Collection<OrderItem> items, String currency) {
        return calculateLineTotals(items.stream().map(OrderItem::getLineTotal).toList(), currency);
    }

    public static Money calculateLineTotals(Collection<Money> lineTotals, String currency) {
        Money total = Money.zero(currency);
        for (Money lineTotal : lineTotals) {
            total = total.add(lineTotal);
        }
        return total;
    }
}
