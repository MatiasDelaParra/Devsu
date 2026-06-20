package com.devsu.account.mapper;

import com.devsu.account.domain.Account;
import com.devsu.account.dto.AccountResponse;
import com.devsu.account.dto.CreateAccountRequest;
import com.devsu.account.dto.UpdateAccountRequest;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toEntity(CreateAccountRequest request) {
        return Account.builder()
                .accountNumber(request.accountNumber())
                .accountType(request.accountType())
                .initialBalance(request.initialBalance())
                .currentBalance(request.initialBalance())
                .status(request.status() == null || request.status())
                .customerId(request.customerId())
                .build();
    }

    public void updateEntity(UpdateAccountRequest request, Account account) {
        account.update(request.accountType(), request.status());
    }

    public AccountResponse toResponse(Account account, String customerName) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getInitialBalance(),
                account.getCurrentBalance(),
                account.getStatus(),
                account.getCustomerId(),
                customerName
        );
    }
}
