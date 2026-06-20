package com.devsu.account.exception;

public class InactiveCustomerException extends BusinessException {

    public InactiveCustomerException(String customerId) {
        super("El cliente " + customerId + " está inactivo");
    }
}
