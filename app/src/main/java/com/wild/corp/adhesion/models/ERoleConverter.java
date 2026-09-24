package com.wild.corp.adhesion.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ERoleConverter implements AttributeConverter<ERole, String> {

    @Override
    public String convertToDatabaseColumn(ERole role) {
        return role == null ? null : role.name();
    }

    @Override
    public ERole convertToEntityAttribute(String value) {
        return value == null ? null : ERole.valueOf(value);
    }
}
