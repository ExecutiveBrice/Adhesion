package com.wild.corp.adhesion.shop.payment.provider;

/** Coordonnées facultatives proposées au payeur dans le checkout. */
public record PaymentPayer(String firstName, String lastName, String email) {
}
