package com.devsu.account.mapper;

import com.devsu.account.domain.Movement;
import com.devsu.account.dto.MovementResponse;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    public MovementResponse toResponse(Movement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getOccurredAt(),
                movement.getMovementType(),
                movement.getValue(),
                movement.getBalance(),
                movement.getAccount().getAccountNumber()
        );
    }
}
