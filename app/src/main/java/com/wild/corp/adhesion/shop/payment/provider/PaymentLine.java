package com.wild.corp.adhesion.shop.payment.provider;

/** Description d'une ligne de commande figée au moment du paiement. */
public record PaymentLine(String productName, String variantName, int quantity) {
}
