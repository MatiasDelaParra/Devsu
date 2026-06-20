package com.devsu.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.AccountType;
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
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String ACCOUNT_NUMBER = "478758";
    private static final String CUSTOMER_ID = "CLI-001";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerSnapshotRepository customerSnapshotRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                customerSnapshotRepository,
                new AccountMapper()
        );
    }

    @Test
    void createsAccountSuccessfully() {
        CreateAccountRequest request = createRequest(true);
        CustomerSnapshot customer = customer(true);
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(false);
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.createAccount(request);

        assertThat(response.accountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(response.accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(response.customerName()).isEqualTo("Jose Lema");
    }

    @Test
    void defaultsStatusToTrue() {
        Account savedAccount = createAndCaptureAccount(createRequest(null));

        assertThat(savedAccount.getStatus()).isTrue();
    }

    @Test
    void initializesCurrentBalanceFromInitialBalance() {
        Account savedAccount = createAndCaptureAccount(createRequest(true));

        assertThat(savedAccount.getCurrentBalance())
                .isEqualByComparingTo(savedAccount.getInitialBalance());
    }

    @Test
    void rejectsDuplicateAccountNumber() {
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(createRequest(true)))
                .isInstanceOf(DuplicateAccountException.class);
        verify(customerSnapshotRepository, never()).findByCustomerId(any());
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingCustomer() {
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(false);
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(createRequest(true)))
                .isInstanceOf(CustomerSnapshotNotFoundException.class);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInactiveCustomer() {
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(false);
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer(false)));

        assertThatThrownBy(() -> accountService.createAccount(createRequest(true)))
                .isInstanceOf(InactiveCustomerException.class);
        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void getsAccountByAccountNumber() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account()));
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer(true)));

        AccountResponse response = accountService.getAccount(ACCOUNT_NUMBER);

        assertThat(response.accountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(response.customerName()).isEqualTo("Jose Lema");
    }

    @Test
    void throwsNotFoundWhenAccountDoesNotExist() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(ACCOUNT_NUMBER))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void updatesAccountTypeAndStatus() {
        Account account = account();
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account));
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer(true)));

        AccountResponse response = accountService.updateAccount(
                ACCOUNT_NUMBER,
                new UpdateAccountRequest(AccountType.CHECKING, false)
        );

        assertThat(response.accountType()).isEqualTo(AccountType.CHECKING);
        assertThat(response.status()).isFalse();
    }

    @Test
    void doesNotAllowUpdatingBalanceDirectly() {
        Account account = account();
        BigDecimal originalInitialBalance = account.getInitialBalance();
        BigDecimal originalCurrentBalance = account.getCurrentBalance();
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account));
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer(true)));

        accountService.updateAccount(
                ACCOUNT_NUMBER,
                new UpdateAccountRequest(AccountType.CHECKING, false)
        );

        assertThat(account.getInitialBalance()).isEqualByComparingTo(originalInitialBalance);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo(originalCurrentBalance);
    }

    private Account createAndCaptureAccount(CreateAccountRequest request) {
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(false);
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer(true)));
        when(accountRepository.saveAndFlush(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.createAccount(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private CreateAccountRequest createRequest(Boolean status) {
        return new CreateAccountRequest(
                ACCOUNT_NUMBER,
                AccountType.SAVINGS,
                new BigDecimal("2000.00"),
                status,
                CUSTOMER_ID
        );
    }

    private Account account() {
        return Account.builder()
                .accountNumber(ACCOUNT_NUMBER)
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("2000.00"))
                .currentBalance(new BigDecimal("2000.00"))
                .status(true)
                .customerId(CUSTOMER_ID)
                .build();
    }

    private CustomerSnapshot customer(boolean active) {
        return CustomerSnapshot.builder()
                .customerId(CUSTOMER_ID)
                .name("Jose Lema")
                .identification("1234567890")
                .status(active)
                .build();
    }
}
