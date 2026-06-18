package com.devsu.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @JsonProperty("nombre")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @JsonProperty("genero")
        @NotBlank(message = "El género es obligatorio")
        @Size(max = 20, message = "El género no puede superar los 20 caracteres")
        String gender,

        @JsonProperty("edad")
        @NotNull(message = "La edad es obligatoria")
        @Min(value = 0, message = "La edad no puede ser negativa")
        Integer age,

        @JsonProperty("identificacion")
        @NotBlank(message = "La identificación es obligatoria")
        @Size(max = 50, message = "La identificación no puede superar los 50 caracteres")
        String identification,

        @JsonProperty("direccion")
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
        String address,

        @JsonProperty("telefono")
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
        String phone,

        @JsonProperty("clienteId")
        @Size(max = 50, message = "El clienteId no puede superar los 50 caracteres")
        String customerId,

        @JsonProperty("contrasena")
        @Size(max = 255, message = "La contraseña no puede superar los 255 caracteres")
        String password,

        @JsonProperty("estado")
        Boolean status
) {
}
