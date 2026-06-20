package com.devsu.account.dto;

import com.devsu.account.domain.AccountType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @JsonProperty("numeroCuenta")
        @NotBlank(message = "El número de cuenta es obligatorio")
        @Size(max = 50, message = "El número de cuenta no puede superar los 50 caracteres")
        String accountNumber,

        @JsonProperty("tipoCuenta")
        @NotNull(message = "El tipo de cuenta es obligatorio")
        AccountType accountType,

        @JsonProperty("saldoInicial")
        @NotNull(message = "El saldo inicial es obligatorio")
        @DecimalMin(value = "0.00", message = "El saldo inicial debe ser cero o positivo")
        BigDecimal initialBalance,

        @JsonProperty("estado")
        Boolean status,

        @JsonProperty("clienteId")
        @NotBlank(message = "El clienteId es obligatorio")
        @Size(max = 50, message = "El clienteId no puede superar los 50 caracteres")
        String customerId
) {
}
