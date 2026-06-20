package com.devsu.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.AccountType;
import com.devsu.account.domain.CustomerSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class AccountPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerSnapshotRepository customerSnapshotRepository;

    @Test
    void persistsAccountWithOptimisticLockVersion() {
        Account account = Account.builder()
                .accountNumber("100000001")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("100.00"))
                .currentBalance(new BigDecimal("100.00"))
                .status(true)
                .customerId("customer-1")
                .build();

        Account persisted = accountRepository.saveAndFlush(account);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getVersion()).isZero();
        assertThat(accountRepository.findByAccountNumber("100000001")).contains(persisted);
        assertThat(accountRepository.findByCustomerId("customer-1")).containsExactly(persisted);
    }

    @Test
    void persistsCustomerSnapshot() {
        Instant occurredAt = Instant.parse("2026-06-20T12:00:00Z");
        CustomerSnapshot snapshot = CustomerSnapshot.builder()
                .customerId("customer-1")
                .name("Jane Doe")
                .identification("1234567890")
                .status(true)
                .createdAt(occurredAt)
                .updatedAt(occurredAt)
                .build();

        CustomerSnapshot persisted = customerSnapshotRepository.saveAndFlush(snapshot);

        assertThat(persisted.getId()).isNotNull();
        assertThat(customerSnapshotRepository.findByCustomerId("customer-1"))
                .contains(persisted);
        assertThat(customerSnapshotRepository.existsByCustomerId("customer-1")).isTrue();
    }
}
