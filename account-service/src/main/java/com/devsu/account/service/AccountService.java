package com.devsu.account.service;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.dto.AccountResponse;
import com.devsu.account.dto.CreateAccountRequest;
import com.devsu.account.dto.UpdateAccountRequest;
import com.devsu.account.exception.AccountNotFoundException;
import com.devsu.account.exception.CustomerSnapshotNotFoundException;
import com.devsu.account.exception.DuplicateAccountException;
import com.devsu.account.exception.InactiveCustomerException;
import com.devsu.account.mapper.AccountMapper;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.CustomerSnapshotRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerSnapshotRepository customerSnapshotRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new DuplicateAccountException(request.accountNumber());
        }

        CustomerSnapshot customer = findRequiredCustomer(request.customerId());
        if (!Boolean.TRUE.equals(customer.getStatus())) {
            throw new InactiveCustomerException(request.customerId());
        }

        try {
            Account account = accountRepository.saveAndFlush(accountMapper.toEntity(request));
            return accountMapper.toResponse(account, customer.getName());
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateAccountException(request.accountNumber());
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        Account account = findRequiredAccount(accountNumber);
        CustomerSnapshot customer = findRequiredCustomer(account.getCustomerId());
        return accountMapper.toResponse(account, customer.getName());
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> listAccounts(Pageable pageable) {
        Page<Account> accounts = accountRepository.findAll(pageable);
        Map<String, CustomerSnapshot> customersById = customerSnapshotRepository
                .findByCustomerIdIn(
                        accounts.stream()
                                .map(Account::getCustomerId)
                                .collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(CustomerSnapshot::getCustomerId, Function.identity()));

        return accounts.map(account -> {
            CustomerSnapshot customer = customersById.get(account.getCustomerId());
            if (customer == null) {
                throw new CustomerSnapshotNotFoundException(account.getCustomerId());
            }
            return accountMapper.toResponse(account, customer.getName());
        });
    }

    @Transactional
    public AccountResponse updateAccount(String accountNumber, UpdateAccountRequest request) {
        Account account = findRequiredAccount(accountNumber);
        accountMapper.updateEntity(request, account);
        CustomerSnapshot customer = findRequiredCustomer(account.getCustomerId());
        return accountMapper.toResponse(account, customer.getName());
    }

    private Account findRequiredAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private CustomerSnapshot findRequiredCustomer(String customerId) {
        return customerSnapshotRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerSnapshotNotFoundException(customerId));
    }
}
