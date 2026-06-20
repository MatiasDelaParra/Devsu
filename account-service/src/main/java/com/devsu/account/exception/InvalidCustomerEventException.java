package com.devsu.account.exception;

public class InvalidCustomerEventException extends RuntimeException {

    public InvalidCustomerEventException(String message) {
        super(message);
    }

    public InvalidCustomerEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
