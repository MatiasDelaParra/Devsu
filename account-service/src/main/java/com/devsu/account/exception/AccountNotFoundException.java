package com.devsu.account.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(String accountNumber) {
        super("No existe una cuenta con número " + accountNumber);
    }
}
