package com.devsu.account.repository;

import com.devsu.account.domain.Movement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, UUID> {

    Page<Movement> findByAccountAccountNumber(String accountNumber, Pageable pageable);

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
