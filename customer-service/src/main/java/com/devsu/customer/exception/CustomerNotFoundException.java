package com.devsu.customer.exception;

public class CustomerNotFoundException extends BusinessException {

    public CustomerNotFoundException(String customerId) {
        super("No se encontró un cliente con clienteId: " + customerId);
    }
}
