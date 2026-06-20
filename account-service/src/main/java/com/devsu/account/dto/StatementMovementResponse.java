package com.devsu.account.dto;

import com.devsu.account.domain.MovementType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StatementMovementResponse(
        UUID id,
        @JsonProperty("fecha") Instant occurredAt,
        @JsonProperty("tipoMovimiento") MovementType movementType,
        @JsonProperty("valor") BigDecimal value,
        @JsonProperty("saldo") BigDecimal balance,
        @JsonProperty("reversoDe") UUID reversalOfId,
        @JsonProperty("motivoReverso") String reversalReason
) {
}
