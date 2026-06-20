package com.devsu.account.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsu.account.domain.Account;
import com.devsu.account.domain.AccountType;
import com.devsu.account.domain.CustomerSnapshot;
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
        movementRepository.deleteAll();
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
