package com.prodwatch.api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransactionStatus status) {
        if (status == null) {
            return null;
        }
        return status == TransactionStatus.void_ ? "void" : status.name();
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return "void".equals(dbValue) ? TransactionStatus.void_ : TransactionStatus.valueOf(dbValue);
    }
}
