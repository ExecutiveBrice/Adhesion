package com.wild.corp.adhesion.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }

        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean special = false;
        for (int codePoint : password.codePoints().toArray()) {
            lower |= Character.isLowerCase(codePoint);
            upper |= Character.isUpperCase(codePoint);
            digit |= Character.isDigit(codePoint);
            special |= !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
            if (Character.isISOControl(codePoint)) {
                return false;
            }
        }
        return lower && upper && digit && special;
    }
}
