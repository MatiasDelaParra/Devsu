package com.devsu.account.repository;

import com.devsu.account.domain.Movement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    Page<Movement> findByAccountAccountNumber(String accountNumber, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select movement
            from Movement movement
            join fetch movement.account
            where movement.id = :movementId
            """)
    Optional<Movement> findByIdForUpdate(@Param("movementId") UUID movementId);

    Optional<Movement> findByReversalOfId(UUID movementId);

    @Query(value = """
            SELECT DISTINCT ON (movement.account_id)
                   movement.account_id AS "accountId",
                   movement.balance AS "balance"
            FROM account.movements movement
            JOIN account.accounts account ON account.id = movement.account_id
            WHERE account.customer_id = :customerId
              AND movement.occurred_at < :toExclusive
            ORDER BY movement.account_id, movement.occurred_at DESC, movement.id DESC
            """, nativeQuery = true)
    List<AccountBalanceSnapshot> findClosingBalances(
            @Param("customerId") String customerId,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select movement
            from Movement movement
            join fetch movement.account account
            where account.customerId = :customerId
              and movement.occurredAt >= :from
              and movement.occurredAt < :toExclusive
            order by movement.occurredAt asc, movement.id asc
            """)
    List<Movement> findForStatement(
            @Param("customerId") String customerId,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );
}
