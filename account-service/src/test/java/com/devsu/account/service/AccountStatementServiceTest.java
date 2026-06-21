package com.devsu.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.AccountType;
import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.domain.Movement;
import com.devsu.account.domain.MovementType;
import com.devsu.account.dto.AccountStatementResponse;
import com.devsu.account.exception.CustomerSnapshotNotFoundException;
import com.devsu.account.repository.AccountBalanceSnapshot;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.CustomerSnapshotRepository;
import com.devsu.account.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountStatementServiceTest {

    private static final String CUSTOMER_ID = "CLI-001";

    @Mock
    private CustomerSnapshotRepository customerSnapshotRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    private AccountStatementService service;

    @BeforeEach
    void setUp() {
        service = new AccountStatementService(
                customerSnapshotRepository,
                accountRepository,
                movementRepository
        );
    }

    @Test
    void groupsMovementsByAccountAndIncludesAccountsWithoutMovements() {
        ReportDateRange range = ReportDateRange.parse("2026-06-01,2026-06-30");
        Account firstAccount = account("100001", new BigDecimal("900.00"));
        Account secondAccount = account("100002", new BigDecimal("500.00"));
        Movement debit = movement(firstAccount);
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.of(customer()));
        when(accountRepository.findByCustomerIdAndCreatedAtBeforeOrderByAccountNumberAsc(
                CUSTOMER_ID,
                range.toExclusive()
        ))
                .thenReturn(List.of(firstAccount, secondAccount));
        when(movementRepository.findClosingBalances(CUSTOMER_ID, range.toExclusive()))
                .thenReturn(List.of(balanceSnapshot(firstAccount.getId(), new BigDecimal("900.00"))));
        when(movementRepository.findForStatement(
                CUSTOMER_ID,
                range.fromInclusive(),
                range.toExclusive()
        )).thenReturn(List.of(debit));

        AccountStatementResponse response = service.generate(CUSTOMER_ID, range);

        assertThat(response.customerName()).isEqualTo("Jose Lema");
        assertThat(response.accounts()).hasSize(2);
        assertThat(response.accounts().get(0).currentBalance()).isEqualByComparingTo("900.00");
        assertThat(response.accounts().get(0).movements()).hasSize(1);
        assertThat(response.accounts().get(0).movements().get(0).value())
                .isEqualByComparingTo("-100.00");
        assertThat(response.accounts().get(1).movements()).isEmpty();
        assertThat(response.accounts().get(1).currentBalance()).isEqualByComparingTo("1000.00");
        verify(movementRepository).findForStatement(
                CUSTOMER_ID,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z")
        );
    }

    @Test
    void rejectsUnknownCustomerBeforeQueryingAccounts() {
        when(customerSnapshotRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(Optional.empty());

        ReportDateRange range = ReportDateRange.parse("2026-06-01,2026-06-30");

        assertThatThrownBy(() -> service.generate(CUSTOMER_ID, range))
                .isInstanceOf(CustomerSnapshotNotFoundException.class);
    }

    private CustomerSnapshot customer() {
        return CustomerSnapshot.builder()
                .customerId(CUSTOMER_ID)
                .name("Jose Lema")
                .identification("1234567890")
                .status(true)
                .build();
    }

    private Account account(String accountNumber, BigDecimal balance) {
        return Account.builder()
                .id(UUID.nameUUIDFromBytes(accountNumber.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .accountNumber(accountNumber)
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("1000.00"))
                .currentBalance(balance)
                .status(true)
                .customerId(CUSTOMER_ID)
                .build();
    }

    private AccountBalanceSnapshot balanceSnapshot(UUID accountId, BigDecimal balance) {
        return new AccountBalanceSnapshot() {
            @Override
            public UUID getAccountId() {
                return accountId;
            }

            @Override
            public BigDecimal getBalance() {
                return balance;
            }
        };
    }

    private Movement movement(Account account) {
        return Movement.builder()
                .occurredAt(Instant.parse("2026-06-15T12:00:00Z"))
                .movementType(MovementType.DEBIT)
                .value(new BigDecimal("-100.00"))
                .balance(new BigDecimal("900.00"))
                .account(account)
                .build();
    }
}
