package com.devsu.account.service;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.Movement;
import com.devsu.account.domain.MovementType;
import com.devsu.account.dto.CreateMovementRequest;
import com.devsu.account.dto.MovementResponse;
import com.devsu.account.dto.ReverseMovementRequest;
import com.devsu.account.exception.AccountNotFoundException;
import com.devsu.account.exception.InactiveAccountException;
import com.devsu.account.exception.InsufficientBalanceException;
import com.devsu.account.exception.InvalidMovementException;
import com.devsu.account.exception.MovementNotFoundException;
import com.devsu.account.mapper.MovementMapper;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.MovementRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovementService {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;

    @Transactional
    public MovementResponse createMovement(CreateMovementRequest request) {
        Account account = accountRepository.findByAccountNumberForUpdate(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.accountNumber()));
        if (!Boolean.TRUE.equals(account.getStatus())) {
            throw new InactiveAccountException(request.accountNumber());
        }
        if (request.value().signum() == 0) {
            throw new InvalidMovementException("El valor del movimiento no puede ser cero");
        }

        BigDecimal balance;
        try {
            balance = account.applyMovement(request.value());
        } catch (IllegalArgumentException exception) {
            throw new InsufficientBalanceException();
        }

        Movement movement = Movement.builder()
                .movementType(request.value().signum() > 0 ? MovementType.CREDIT : MovementType.DEBIT)
                .value(request.value())
                .balance(balance)
                .account(account)
                .build();
        return movementMapper.toResponse(movementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public MovementResponse getMovement(UUID movementId) {
        return movementRepository.findById(movementId)
                .map(movementMapper::toResponse)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
    }

    @Transactional
    public MovementResponse reverseMovement(UUID movementId, ReverseMovementRequest request) {
        Movement original = movementRepository.findByIdForUpdate(movementId)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
        if (original.isReversal()) {
            throw new InvalidMovementException("Un movimiento de reverso no puede ser reversado");
        }

        return movementRepository.findByReversalOfId(movementId)
                .map(movementMapper::toResponse)
                .orElseGet(() -> createReversal(original, request.reason()));
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> listMovements(String accountNumber, Pageable pageable) {
        Page<Movement> movements = accountNumber == null || accountNumber.isBlank()
                ? movementRepository.findAll(pageable)
                : movementRepository.findByAccountAccountNumber(accountNumber, pageable);
        return movements.map(movementMapper::toResponse);
    }

    private MovementResponse createReversal(Movement original, String reason) {
        String accountNumber = original.getAccount().getAccountNumber();
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        if (!Boolean.TRUE.equals(account.getStatus())) {
            throw new InactiveAccountException(accountNumber);
        }

        BigDecimal reversalValue = original.getValue().negate();
        BigDecimal balance;
        try {
            balance = account.applyMovement(reversalValue);
        } catch (IllegalArgumentException exception) {
            throw new InsufficientBalanceException();
        }

        Movement reversal = Movement.builder()
                .movementType(reversalValue.signum() > 0 ? MovementType.CREDIT : MovementType.DEBIT)
                .value(reversalValue)
                .balance(balance)
                .account(account)
                .reversalOf(original)
                .reversalReason(reason.trim())
                .build();
        return movementMapper.toResponse(movementRepository.save(reversal));
    }
}
