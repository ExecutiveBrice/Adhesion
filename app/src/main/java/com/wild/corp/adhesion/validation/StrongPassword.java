package com.wild.corp.adhesion.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Le mot de passe doit contenir 12 caractères minimum, une majuscule, une minuscule, un chiffre et un caractère spécial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
