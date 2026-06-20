package com.devsu.account.dto;

import com.devsu.account.domain.AccountType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateAccountRequest(
        @JsonProperty("tipoCuenta")
        AccountType accountType,

        @JsonProperty("estado")
        Boolean status
) {
}
