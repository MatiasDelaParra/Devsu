package com.devsu.account.dto;

import com.devsu.account.domain.AccountType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record AccountResponse(
        @JsonProperty("numeroCuenta")
        String accountNumber,

        @JsonProperty("tipoCuenta")
        AccountType accountType,

        @JsonProperty("saldoInicial")
        BigDecimal initialBalance,

        @JsonProperty("saldoDisponible")
        BigDecimal currentBalance,

        @JsonProperty("estado")
        Boolean status,

        @JsonProperty("clienteId")
        String customerId,

        @JsonProperty("cliente")
        String customerName
) {
}
