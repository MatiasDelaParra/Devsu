package com.devsu.account.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.AccountType;
import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.domain.Movement;
import com.devsu.account.domain.MovementType;
import com.devsu.account.repository.AccountRepository;
import com.devsu.account.repository.CustomerSnapshotRepository;
import com.devsu.account.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AccountApiIntegrationTest {

    private static final String CUSTOMER_ID = "CLI-001";

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
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerSnapshotRepository customerSnapshotRepository;

    @Autowired
    private MovementRepository movementRepository;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAllInBatch();
        accountRepository.deleteAll();
        customerSnapshotRepository.deleteAll();
        customerSnapshotRepository.save(activeCustomer());
    }

    @Test
    void postCreatesAccount() throws Exception {
        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("478758", CUSTOMER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cuentas/478758"))
                .andExpect(jsonPath("$.numeroCuenta").value("478758"))
                .andExpect(jsonPath("$.tipoCuenta").value("Ahorros"))
                .andExpect(jsonPath("$.saldoInicial").value(2000))
                .andExpect(jsonPath("$.saldoDisponible").value(2000))
                .andExpect(jsonPath("$.estado").value(true))
                .andExpect(jsonPath("$.clienteId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.cliente").value("Jose Lema"));
    }

    @Test
    void postReturnsConflictForDuplicateAccount() throws Exception {
        accountRepository.save(account("478758"));

        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("478758", CUSTOMER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflicto"))
                .andExpect(jsonPath("$.path").value("/api/cuentas"));
    }

    @Test
    void postReturnsNotFoundForMissingCustomer() throws Exception {
        mockMvc.perform(post("/api/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest("478758", "MISSING")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No Encontrado"));
    }

    @Test
    void getReturnsAccount() throws Exception {
        accountRepository.save(account("478758"));

        mockMvc.perform(get("/api/cuentas/478758"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuenta").value("478758"))
                .andExpect(jsonPath("$.cliente").value("Jose Lema"));
    }

    @Test
    void getReturnsPaginatedAccounts() throws Exception {
        accountRepository.save(account("478758"));
        accountRepository.save(account("225487"));

        mockMvc.perform(get("/api/cuentas")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void putUpdatesAccountTypeAndStatus() throws Exception {
        accountRepository.save(account("478758"));

        mockMvc.perform(put("/api/cuentas/478758")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoCuenta": "Corriente",
                                  "estado": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoCuenta").value("Corriente"))
                .andExpect(jsonPath("$.estado").value(false))
                .andExpect(jsonPath("$.saldoInicial").value(2000))
                .andExpect(jsonPath("$.saldoDisponible").value(2000));
    }

    @Test
    void putRejectsDirectBalanceUpdate() throws Exception {
        accountRepository.save(account("478758"));

        mockMvc.perform(put("/api/cuentas/478758")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoCuenta": "Corriente",
                                  "saldoDisponible": 5000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void postMovementUpdatesBalanceAndPersistsTransaction() throws Exception {
        accountRepository.save(account("478758"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroCuenta": "478758",
                                  "valor": -575
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("DEBIT"))
                .andExpect(jsonPath("$.valor").value(-575))
                .andExpect(jsonPath("$.saldo").value(1425))
                .andExpect(jsonPath("$.numeroCuenta").value("478758"));

        mockMvc.perform(get("/api/cuentas/478758"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDisponible").value(1425));
    }

    @Test
    void postMovementReturnsExpectedErrorWhenBalanceIsInsufficient() throws Exception {
        accountRepository.save(account("478758"));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroCuenta": "478758",
                                  "valor": -2001
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Saldo no disponible"));

        mockMvc.perform(get("/api/cuentas/478758"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDisponible").value(2000));
    }

    @Test
    void putMovementReversalRestoresBalanceAndIsIdempotent() throws Exception {
        accountRepository.save(account("478758"));
        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numeroCuenta": "478758",
                                  "valor": -575
                                }
                                """))
                .andExpect(status().isCreated());
        var original = movementRepository.findAll().getFirst();

        mockMvc.perform(put("/api/movimientos/{id}/reverso", original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo": "Operación duplicada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMovimiento").value("CREDIT"))
                .andExpect(jsonPath("$.valor").value(575))
                .andExpect(jsonPath("$.saldo").value(2000))
                .andExpect(jsonPath("$.reversoDe").value(original.getId().toString()))
                .andExpect(jsonPath("$.motivoReverso").value("Operación duplicada"));

        mockMvc.perform(put("/api/movimientos/{id}/reverso", original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivo": "Solicitud repetida"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoReverso").value("Operación duplicada"));

        mockMvc.perform(get("/api/cuentas/478758"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoDisponible").value(2000));
        assertThat(movementRepository.count()).isEqualTo(2);
    }

    @Test
    void getReportReturnsAccountsAndOnlyMovementsInsideInclusiveRange() throws Exception {
        Account firstAccount = accountRepository.save(account("100001"));
        accountRepository.save(account("100002"));
        movementRepository.save(movement(
                firstAccount,
                "2026-06-01T00:00:00Z",
                new BigDecimal("-100.00"),
                new BigDecimal("1900.00")
        ));
        movementRepository.save(movement(
                firstAccount,
                "2026-06-30T23:59:59Z",
                new BigDecimal("50.00"),
                new BigDecimal("1950.00")
        ));
        movementRepository.save(movement(
                firstAccount,
                "2026-07-01T00:00:00Z",
                new BigDecimal("25.00"),
                new BigDecimal("1975.00")
        ));

        mockMvc.perform(get("/api/reportes")
                        .param("fecha", "2026-06-01,2026-06-30")
                        .param("cliente", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.cliente").value("Jose Lema"))
                .andExpect(jsonPath("$.fechaDesde").value("2026-06-01"))
                .andExpect(jsonPath("$.fechaHasta").value("2026-06-30"))
                .andExpect(jsonPath("$.cuentas.length()").value(2))
                .andExpect(jsonPath("$.cuentas[0].numeroCuenta").value("100001"))
                .andExpect(jsonPath("$.cuentas[0].saldoDisponible").value(1950))
                .andExpect(jsonPath("$.cuentas[0].movimientos.length()").value(2))
                .andExpect(jsonPath("$.cuentas[0].movimientos[0].valor").value(-100))
                .andExpect(jsonPath("$.cuentas[0].movimientos[1].valor").value(50))
                .andExpect(jsonPath("$.cuentas[1].numeroCuenta").value("100002"))
                .andExpect(jsonPath("$.cuentas[1].saldoDisponible").value(2000))
                .andExpect(jsonPath("$.cuentas[1].movimientos.length()").value(0));
    }

    @Test
    void getReportReturnsHistoricalClosingBalanceInsteadOfCurrentBalance() throws Exception {
        Account historicalAccount = accountRepository.save(Account.builder()
                .accountNumber("300001")
                .accountType(AccountType.CHECKING)
                .initialBalance(new BigDecimal("100.00"))
                .currentBalance(new BigDecimal("700.00"))
                .status(true)
                .customerId(CUSTOMER_ID)
                .createdAt(Instant.parse("2019-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-06-20T12:00:00Z"))
                .build());
        movementRepository.save(movement(
                historicalAccount,
                "2026-06-20T12:00:00Z",
                new BigDecimal("600.00"),
                new BigDecimal("700.00")
        ));

        mockMvc.perform(get("/api/reportes")
                        .param("fecha", "2020-01-01,2020-01-31")
                        .param("cliente", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentas.length()").value(1))
                .andExpect(jsonPath("$.cuentas[0].numeroCuenta").value("300001"))
                .andExpect(jsonPath("$.cuentas[0].saldoInicial").value(100))
                .andExpect(jsonPath("$.cuentas[0].saldoDisponible").value(100))
                .andExpect(jsonPath("$.cuentas[0].movimientos.length()").value(0));
    }

    @Test
    void getReportExcludesAccountsCreatedAfterRequestedPeriod() throws Exception {
        accountRepository.save(Account.builder()
                .accountNumber("300002")
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("500.00"))
                .currentBalance(new BigDecimal("500.00"))
                .status(true)
                .customerId(CUSTOMER_ID)
                .createdAt(Instant.parse("2021-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2021-01-01T00:00:00Z"))
                .build());

        mockMvc.perform(get("/api/reportes")
                        .param("fecha", "2020-01-01,2020-01-31")
                        .param("cliente", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuentas.length()").value(0));
    }

    @Test
    void getReportRejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/reportes")
                        .param("fecha", "2026-06-30,2026-06-01")
                        .param("cliente", CUSTOMER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La fecha inicial no puede ser posterior a la fecha final"));
    }

    @Test
    void getReportReturnsNotFoundForUnknownCustomer() throws Exception {
        mockMvc.perform(get("/api/reportes")
                        .param("fecha", "2026-06-01,2026-06-30")
                        .param("cliente", "MISSING"))
                .andExpect(status().isNotFound());
    }

    private CustomerSnapshot activeCustomer() {
        Instant now = Instant.parse("2026-06-20T12:00:00Z");
        return CustomerSnapshot.builder()
                .customerId(CUSTOMER_ID)
                .name("Jose Lema")
                .identification("1234567890")
                .status(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Account account(String accountNumber) {
        return Account.builder()
                .accountNumber(accountNumber)
                .accountType(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("2000.00"))
                .currentBalance(new BigDecimal("2000.00"))
                .status(true)
                .customerId(CUSTOMER_ID)
                .build();
    }

    private Movement movement(
            Account account,
            String occurredAt,
            BigDecimal value,
            BigDecimal balance
    ) {
        return Movement.builder()
                .occurredAt(Instant.parse(occurredAt))
                .movementType(value.signum() > 0 ? MovementType.CREDIT : MovementType.DEBIT)
                .value(value)
                .balance(balance)
                .account(account)
                .build();
    }

    private String createRequest(String accountNumber, String customerId) {
        return """
                {
                  "numeroCuenta": "%s",
                  "tipoCuenta": "Ahorros",
                  "saldoInicial": 2000,
                  "estado": true,
                  "clienteId": "%s"
                }
                """.formatted(accountNumber, customerId);
    }
}
