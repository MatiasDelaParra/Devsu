package com.devsu.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.AccountType;
import com.devsu.account.domain.Movement;
import com.devsu.account.domain.MovementType;
import com.devsu.account.dto.CreateMovementRequest;
import com.devsu.account.dto.MovementResponse;
import com.devsu.account.exception.InsufficientBalanceException;
import com.devsu.account.exception.InvalidMovementException;
import com.devsu.account.mapper.MovementMapper;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    private static final String ACCOUNT_NUMBER = "478758";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    private MovementService movementService;

    @BeforeEach
    void setUp() {
        movementService = new MovementService(
                accountRepository,
                movementRepository,
                new MovementMapper()
        );
    }

    @Test
    void createsCreditAndUpdatesBalance() {
        Account account = account(new BigDecimal("100.00"));
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovementResponse response = movementService.createMovement(
                new CreateMovementRequest(ACCOUNT_NUMBER, new BigDecimal("50.00"))
        );

        assertThat(response.movementType()).isEqualTo(MovementType.CREDIT);
        assertThat(response.balance()).isEqualByComparingTo("150.00");
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void createsDebitAndUpdatesBalance() {
        Account account = account(new BigDecimal("100.00"));
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MovementResponse response = movementService.createMovement(
                new CreateMovementRequest(ACCOUNT_NUMBER, new BigDecimal("-40.00"))
        );

        assertThat(response.movementType()).isEqualTo(MovementType.DEBIT);
        assertThat(response.balance()).isEqualByComparingTo("60.00");
    }

    @Test
    void rejectsMovementWhenBalanceIsInsufficient() {
        Account account = account(new BigDecimal("100.00"));
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> movementService.createMovement(
                new CreateMovementRequest(ACCOUNT_NUMBER, new BigDecimal("-100.01"))
        ))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Saldo no disponible");

        assertThat(account.getCurrentBalance()).isEqualByComparingTo("100.00");
        verify(movementRepository, never()).save(any());
    }

    @Test
    void rejectsZeroValue() {
        when(accountRepository.findByAccountNumberForUpdate(ACCOUNT_NUMBER))
                .thenReturn(Optional.of(account(new BigDecimal("100.00"))));

        assertThatThrownBy(() -> movementService.createMovement(
                new CreateMovementRequest(ACCOUNT_NUMBER, BigDecimal.ZERO)
        )).isInstanceOf(InvalidMovementException.class);

        verify(movementRepository, never()).save(any());
    }

    private Account account(BigDecimal balance) {
        return Account.builder()
                .accountNumber(ACCOUNT_NUMBER)
                .accountType(AccountType.SAVINGS)
                .initialBalance(balance)
                .currentBalance(balance)
                .status(true)
                .customerId("CLI-001")
                .createdAt(Instant.parse("2026-06-20T12:00:00Z"))
                .updatedAt(Instant.parse("2026-06-20T12:00:00Z"))
                .build();
    }
}
