package com.devsu.account.service;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.domain.Movement;
import com.devsu.account.dto.AccountStatementResponse;
import com.devsu.account.dto.StatementAccountResponse;
import com.devsu.account.dto.StatementMovementResponse;
import com.devsu.account.exception.CustomerSnapshotNotFoundException;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.CustomerSnapshotRepository;
import com.devsu.account.repository.MovementRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

    private final CustomerSnapshotRepository customerSnapshotRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    @Transactional(readOnly = true)
    public AccountStatementResponse generate(String customerId, ReportDateRange range) {
        CustomerSnapshot customer = customerSnapshotRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerSnapshotNotFoundException(customerId));
        List<Account> accounts = accountRepository.findByCustomerIdOrderByAccountNumberAsc(customerId);
        Map<String, List<Movement>> movementsByAccount = movementRepository.findForStatement(
                        customerId,
                        range.fromInclusive(),
                        range.toExclusive()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        movement -> movement.getAccount().getAccountNumber()
                ));

        List<StatementAccountResponse> accountStatements = accounts.stream()
                .map(account -> toAccountStatement(
                        account,
                        movementsByAccount.getOrDefault(account.getAccountNumber(), List.of())
                ))
                .toList();

        return new AccountStatementResponse(
                customer.getCustomerId(),
                customer.getName(),
                range.from(),
                range.to(),
                accountStatements
        );
    }

    private StatementAccountResponse toAccountStatement(
            Account account,
            List<Movement> movements
    ) {
        return new StatementAccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getInitialBalance(),
                account.getCurrentBalance(),
                account.getStatus(),
                movements.stream().map(this::toMovementResponse).toList()
        );
    }

    private StatementMovementResponse toMovementResponse(Movement movement) {
        return new StatementMovementResponse(
                movement.getId(),
                movement.getOccurredAt(),
                movement.getMovementType(),
                movement.getValue(),
                movement.getBalance()
        );
    }
}
