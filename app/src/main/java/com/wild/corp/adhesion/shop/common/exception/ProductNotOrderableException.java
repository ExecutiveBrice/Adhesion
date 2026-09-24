package com.wild.corp.adhesion.shop.common.exception;

public class ProductNotOrderableException extends IllegalStateException {

    public ProductNotOrderableException(Long productId) {
        super("Le produit " + productId + " n'est pas disponible à la commande");
    }
}
