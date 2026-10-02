package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.catalog.model.SupplierOrderStatus;

import java.time.Instant;
import java.util.List;

public record SupplierOrderResponse(Long id, String supplierName, String reference,
                                    SupplierOrderStatus status, Instant createdAt, Instant receivedAt,
                                    List<Line> lines) {

    public record Line(Long id, Long variantId, String productName, String variantName,
                       String sku, int quantity, Long unitCostAmountInCents,
                       String unitCostCurrency, Long lineTotalAmountInCents, Integer expectedNeed,
                       boolean extraApproved) { }
}
