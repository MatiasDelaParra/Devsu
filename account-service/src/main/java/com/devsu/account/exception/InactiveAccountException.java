package com.devsu.account.exception;

public class InactiveAccountException extends BusinessException {

    public InactiveAccountException(String accountNumber) {
        super("La cuenta " + accountNumber + " está inactiva");
    }
}
