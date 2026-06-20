package com.devsu.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReverseMovementRequest(
        @JsonProperty("motivo")
        @NotBlank(message = "El motivo del reverso es obligatorio")
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres")
        String reason
) {
}
