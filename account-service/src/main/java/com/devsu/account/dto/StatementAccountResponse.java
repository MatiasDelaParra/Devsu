package com.devsu.account.dto;

import com.devsu.account.domain.AccountType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record StatementAccountResponse(
        @JsonProperty("numeroCuenta") String accountNumber,
        @JsonProperty("tipoCuenta") AccountType accountType,
        @JsonProperty("saldoInicial") BigDecimal initialBalance,
        @JsonProperty("saldoDisponible") BigDecimal currentBalance,
        @JsonProperty("estado") Boolean status,
        @JsonProperty("movimientos") List<StatementMovementResponse> movements
) {
}
