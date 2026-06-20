package com.devsu.account.exception;

public class CustomerSnapshotNotFoundException extends BusinessException {

    public CustomerSnapshotNotFoundException(String customerId) {
        super("No existe un cliente con identificador " + customerId);
    }
}
