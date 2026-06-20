package com.devsu.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record AccountStatementResponse(
        @JsonProperty("clienteId") String customerId,
        @JsonProperty("cliente") String customerName,
        @JsonProperty("fechaDesde") LocalDate from,
        @JsonProperty("fechaHasta") LocalDate to,
        @JsonProperty("cuentas") List<StatementAccountResponse> accounts
) {
}
