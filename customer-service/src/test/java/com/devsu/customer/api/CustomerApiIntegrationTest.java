package com.devsu.customer.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsu.customer.outbox.OutboxEventRepository;
import com.devsu.customer.repository.CustomerRepository;
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
        "customer.messaging.publisher.initial-delay=3600000",
        "customer.messaging.publisher.fixed-delay=3600000"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CustomerApiIntegrationTest {

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
    private CustomerRepository customerRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
    }

    @Test
    void createsCustomerAndOutboxEventAtomically() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value("CUS-001"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        var customer = customerRepository.findByCustomerId("CUS-001").orElseThrow();
        assertThat(customer.getPassword()).startsWith("$2");
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsChangingCustomerIdAndPreservesOriginalIdentity() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/clientes/CUS-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Jose Updated",
                                  "genero": "Masculino",
                                  "edad": 36,
                                  "identificacion": "1710000001",
                                  "direccion": "Nueva direccion",
                                  "telefono": "098254785",
                                  "clienteId": "CUS-002",
                                  "estado": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El clienteId no puede modificarse"));

        assertThat(customerRepository.findByCustomerId("CUS-001")).isPresent();
        assertThat(customerRepository.findByCustomerId("CUS-002")).isEmpty();
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    private String createRequest() {
        return """
                {
                  "nombre": "Jose Lema",
                  "genero": "Masculino",
                  "edad": 35,
                  "identificacion": "1710000001",
                  "direccion": "Otavalo sn y principal",
                  "telefono": "098254785",
                  "clienteId": "CUS-001",
                  "contrasena": "1234",
                  "estado": true
                }
                """;
    }
}
