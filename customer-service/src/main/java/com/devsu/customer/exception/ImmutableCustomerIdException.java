package com.devsu.customer.exception;

public class ImmutableCustomerIdException extends BusinessException {

    public ImmutableCustomerIdException() {
        super("El clienteId no puede modificarse");
    }
}
