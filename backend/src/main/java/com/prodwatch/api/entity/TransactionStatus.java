package com.prodwatch.api.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionStatus {
    completed,
    refunded,
    void_;

    @JsonValue
    public String toJson() {
        return this == void_ ? "void" : name();
    }

    @JsonCreator
    public static TransactionStatus fromJson(String value) {
        return "void".equals(value) ? void_ : valueOf(value);
    }
}
