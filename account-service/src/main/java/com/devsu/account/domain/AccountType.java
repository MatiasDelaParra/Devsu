package com.devsu.account.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum AccountType {
    SAVINGS("Ahorros"),
    CHECKING("Corriente");

    private final String apiValue;

    AccountType(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String getApiValue() {
        return apiValue;
    }

    @JsonCreator
    public static AccountType fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.apiValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "tipoCuenta debe ser Ahorros o Corriente"
                ));
    }
}
