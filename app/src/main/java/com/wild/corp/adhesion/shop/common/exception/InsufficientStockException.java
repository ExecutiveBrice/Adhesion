package com.wild.corp.adhesion.shop.common.exception;

public class InsufficientStockException extends IllegalStateException {

    public InsufficientStockException(Long variantId, long available, int requested) {
        super("Stock insuffisant pour la variante " + variantId
                + " : " + available + " disponible(s), " + requested + " demandé(s)");
    }
}
