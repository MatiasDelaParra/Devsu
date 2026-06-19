package com.devsu.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomerStatusRequest(
        @JsonProperty("estado")
        @NotNull(message = "El estado es obligatorio")
        Boolean status
) {
}
