package com.devsu.account.exception;

public class DuplicateAccountException extends BusinessException {

    public DuplicateAccountException(String accountNumber) {
        super("Ya existe una cuenta con número " + accountNumber);
    }
}
