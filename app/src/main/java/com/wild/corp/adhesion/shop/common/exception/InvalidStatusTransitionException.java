package com.wild.corp.adhesion.shop.common.exception;

public class InvalidStatusTransitionException extends IllegalStateException {

    public InvalidStatusTransitionException(Enum<?> currentStatus, Enum<?> targetStatus) {
        super("Transition de statut interdite : " + currentStatus + " -> " + targetStatus);
    }
}
