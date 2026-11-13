package com.wild.corp.adhesion.shop.api.dto;

import java.util.List;

public record CartQuoteResponse(List<CartLineResponse> items,
                                MoneyResponse subtotal,
                                MoneyResponse discountTotal,
                                MoneyResponse feesTotal,
                                MoneyResponse total) {
}
