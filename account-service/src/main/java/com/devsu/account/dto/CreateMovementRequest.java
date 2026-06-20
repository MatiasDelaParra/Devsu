package com.devsu.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateMovementRequest(
        @JsonProperty("numeroCuenta")
        @NotBlank(message = "El número de cuenta es obligatorio")
        @Size(max = 50, message = "El número de cuenta no puede superar los 50 caracteres")
        String accountNumber,

        @JsonProperty("valor")
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "-99999999999999999.99", message = "El valor está fuera del rango permitido")
        @DecimalMax(value = "99999999999999999.99", message = "El valor está fuera del rango permitido")
        BigDecimal value
) {
}
