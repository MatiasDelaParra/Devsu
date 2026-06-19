package com.devsu.customer.exception;

public class DuplicateCustomerException extends BusinessException {

    private DuplicateCustomerException(String message) {
        super(message);
    }

    public static DuplicateCustomerException forIdentification(String identification) {
        return new DuplicateCustomerException(
                "Ya existe un cliente con la identificación: " + identification
        );
    }

    public static DuplicateCustomerException forCustomerId(String customerId) {
        return new DuplicateCustomerException(
                "Ya existe un cliente con el clienteId: " + customerId
        );
    }
}
