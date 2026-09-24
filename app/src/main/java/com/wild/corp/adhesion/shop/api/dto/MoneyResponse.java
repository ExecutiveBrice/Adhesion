package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.common.money.Money;

public record MoneyResponse(long amountInCents, String currency) {

    public static MoneyResponse from(Money money) {
        return new MoneyResponse(money.getAmountInCents(), money.getCurrency());
    }
}
