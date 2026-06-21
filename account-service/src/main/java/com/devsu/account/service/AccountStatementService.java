package com.devsu.account.service;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.domain.Movement;
import com.devsu.account.dto.AccountStatementResponse;
import com.devsu.account.dto.StatementAccountResponse;
import com.devsu.account.dto.StatementMovementResponse;
import com.devsu.account.exception.CustomerSnapshotNotFoundException;
import com.devsu.account.repository.AccountBalanceSnapshot;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.CustomerSnapshotRepository;
import com.devsu.account.repository.MovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

    private final CustomerSnapshotRepository customerSnapshotRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    @Transactional(readOnly = true)
    public AccountStatementResponse generate(String customerId, ReportDateRange range) {
        CustomerSnapshot customer = findCustomer(customerId);
        List<Account> accounts = findAccountsIncludedInRange(customerId, range);
        Map<UUID, BigDecimal> closingBalances = findClosingBalances(customerId, range);
        Map<UUID, List<Movement>> movementsByAccount = findMovementsByAccount(customerId, range);

        List<StatementAccountResponse> accountStatements = accounts.stream()
                .map(account -> toAccountStatement(
                        account,
                        closingBalanceFor(account, closingBalances),
                        movementsFor(account, movementsByAccount)
                ))
                .toList();

        return toStatementResponse(customer, range, accountStatements);
    }

    private CustomerSnapshot findCustomer(String customerId) {
        return customerSnapshotRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerSnapshotNotFoundException(customerId));
    }

    private List<Account> findAccountsIncludedInRange(String customerId, ReportDateRange range) {
        return accountRepository.findByCustomerIdAndCreatedAtBeforeOrderByAccountNumberAsc(
                customerId,
                range.toExclusive()
        );
    }

    private Map<UUID, BigDecimal> findClosingBalances(String customerId, ReportDateRange range) {
        return movementRepository.findClosingBalances(customerId, range.toExclusive())
                .stream()
                .collect(Collectors.toMap(
                        AccountBalanceSnapshot::getAccountId,
                        AccountBalanceSnapshot::getBalance
                ));
    }

    private Map<UUID, List<Movement>> findMovementsByAccount(String customerId, ReportDateRange range) {
        return movementRepository.findForStatement(
                        customerId,
                        range.fromInclusive(),
                        range.toExclusive()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        movement -> movement.getAccount().getId()
                ));
    }

    private BigDecimal closingBalanceFor(
            Account account,
            Map<UUID, BigDecimal> closingBalances
    ) {
        return closingBalances.getOrDefault(account.getId(), account.getInitialBalance());
    }

    private List<Movement> movementsFor(
            Account account,
            Map<UUID, List<Movement>> movementsByAccount
    ) {
        return movementsByAccount.getOrDefault(account.getId(), List.of());
    }

    private AccountStatementResponse toStatementResponse(
            CustomerSnapshot customer,
            ReportDateRange range,
            List<StatementAccountResponse> accounts
    ) {
        return new AccountStatementResponse(
                customer.getCustomerId(),
                customer.getName(),
                range.from(),
                range.to(),
                accounts
        );
    }

    private StatementAccountResponse toAccountStatement(
            Account account,
            BigDecimal closingBalance,
            List<Movement> movements
    ) {
        return new StatementAccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getInitialBalance(),
                closingBalance,
                account.getStatus(),
                movements.stream()
                        .map(this::toMovementResponse)
                        .toList()
        );
    }

    private StatementMovementResponse toMovementResponse(Movement movement) {
        return new StatementMovementResponse(
                movement.getId(),
                movement.getOccurredAt(),
                movement.getMovementType(),
                movement.getValue(),
                movement.getBalance(),
                movement.getReversalOf() == null ? null : movement.getReversalOf().getId(),
                movement.getReversalReason()
        );
    }
}
