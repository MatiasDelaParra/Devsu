package com.devsu.account.exception;

import java.util.UUID;

public class MovementNotFoundException extends BusinessException {

    public MovementNotFoundException(UUID movementId) {
        super("No existe un movimiento con id " + movementId);
    }
}
