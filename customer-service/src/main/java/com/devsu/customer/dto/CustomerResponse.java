package com.devsu.customer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record CustomerResponse(
        UUID id,

        @JsonProperty("nombre")
        String name,

        @JsonProperty("genero")
        String gender,

        @JsonProperty("edad")
        Integer age,

        @JsonProperty("identificacion")
        String identification,

        @JsonProperty("direccion")
        String address,

        @JsonProperty("telefono")
        String phone,

        @JsonProperty("clienteId")
        String customerId,

        @JsonProperty("estado")
        Boolean status
) {
}
