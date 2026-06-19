package com.devsu.customer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.exception.CustomerNotFoundException;
import com.devsu.customer.exception.DuplicateCustomerException;
import com.devsu.customer.mapper.CustomerMapper;
import com.devsu.customer.service.CustomerService;
import com.devsu.customer.service.command.CreateCustomerCommand;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private CustomerMapper customerMapper;

    @Test
    void createsCustomerWithSpanishJsonContract() throws Exception {
        Customer customer = customer();
        CustomerResponse response = response();

        CreateCustomerCommand command = createCommand();
        when(customerMapper.toCommand(any(CreateCustomerRequest.class))).thenReturn(command);
        when(customerService.createCustomer(command)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(response);

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "John Smith",
                                  "genero": "MALE",
                                  "edad": 32,
                                  "identificacion": "0102030405",
                                  "direccion": "123 Main Avenue",
                                  "telefono": "0999999999",
                                  "clienteId": "CUS-001",
                                  "contrasena": "secret"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/clientes/CUS-001"))
                .andExpect(jsonPath("$.nombre").value("John Smith"))
                .andExpect(jsonPath("$.clienteId").value("CUS-001"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void returnsBadRequestForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": "CUS-001",
                                  "contrasena": "secret"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Solicitud Incorrecta"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("obligatorio")))
                .andExpect(jsonPath("$.path").value("/api/clientes"));
    }

    @Test
    void returnsNotFoundForMissingCustomer() throws Exception {
        when(customerService.getCustomerByCustomerId("CUS-404"))
                .thenThrow(new CustomerNotFoundException("CUS-404"));

        mockMvc.perform(get("/api/clientes/CUS-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("No Encontrado"))
                .andExpect(jsonPath("$.message").value("No se encontró un cliente con clienteId: CUS-404"))
                .andExpect(jsonPath("$.path").value("/api/clientes/CUS-404"));
    }

    @Test
    void returnsConflictInSpanishForDuplicateCustomer() throws Exception {
        CreateCustomerCommand command = createCommand();

        when(customerMapper.toCommand(any(CreateCustomerRequest.class))).thenReturn(command);
        when(customerService.createCustomer(command))
                .thenThrow(DuplicateCustomerException.forIdentification("0102030405"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflicto"))
                .andExpect(jsonPath("$.message")
                        .value("Ya existe un cliente con la identificación: 0102030405"))
                .andExpect(jsonPath("$.path").value("/api/clientes"));
    }

    @Test
    void returnsSpanishMessageForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Solicitud Incorrecta"))
                .andExpect(jsonPath("$.message")
                        .value("El cuerpo de la solicitud no es válido o está mal formado"));
    }

    private CreateCustomerCommand createCommand() {
        return CreateCustomerCommand.builder()
                .name("John Smith")
                .gender("MALE")
                .age(32)
                .identification("0102030405")
                .address("123 Main Avenue")
                .phone("0999999999")
                .customerId("CUS-001")
                .password("secret")
                .build();
    }

    private String validCreateRequest() {
        return """
                {
                  "nombre": "John Smith",
                  "genero": "MALE",
                  "edad": 32,
                  "identificacion": "0102030405",
                  "direccion": "123 Main Avenue",
                  "telefono": "0999999999",
                  "clienteId": "CUS-001",
                  "contrasena": "secret"
                }
                """;
    }

    private Customer customer() {
        return Customer.builder()
                .name("John Smith")
                .gender("MALE")
                .age(32)
                .identification("0102030405")
                .address("123 Main Avenue")
                .phone("0999999999")
                .customerId("CUS-001")
                .password("encoded-secret")
                .status(true)
                .build();
    }

    private CustomerResponse response() {
        return new CustomerResponse(
                UUID.randomUUID(),
                "John Smith",
                "MALE",
                32,
                "0102030405",
                "123 Main Avenue",
                "0999999999",
                "CUS-001",
                true
        );
    }
}
