package com.wild.corp.adhesion.shop.common.exception;

public class InvalidQuantityException extends IllegalArgumentException {

    public InvalidQuantityException(int quantity) {
        super("La quantité doit être strictement positive, valeur reçue : " + quantity);
    }
}
