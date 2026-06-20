package com.devsu.account.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException() {
        super("Saldo no disponible");
    }
}
