package com.example.pipemate.preset;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null) return null;
        return "{" + String.join(",", attribute) + "}";
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.length() < 2) return new String[0];
        return dbData.substring(1, dbData.length() - 1).split(",");
    }
}