package com.devsu.customer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.dto.UpdateCustomerRequest;
import com.devsu.customer.domain.Customer;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerMapperTest {

    private final CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void mapsCreateRequestToCommand() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John Smith",
                "MALE",
                32,
                "0102030405",
                "123 Main Avenue",
                "0999999999",
                "CUS-001",
                "secret",
                true
        );

        CreateCustomerCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("John Smith");
        assertThat(command.gender()).isEqualTo("MALE");
        assertThat(command.age()).isEqualTo(32);
        assertThat(command.identification()).isEqualTo("0102030405");
        assertThat(command.address()).isEqualTo("123 Main Avenue");
        assertThat(command.phone()).isEqualTo("0999999999");
        assertThat(command.customerId()).isEqualTo("CUS-001");
        assertThat(command.password()).isEqualTo("secret");
        assertThat(command.status()).isTrue();
    }

    @Test
    void mapsUpdateRequestToCommand() {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "John Updated",
                "MALE",
                33,
                "0102030405",
                "456 New Avenue",
                "0988888888",
                null,
                "new-secret",
                false
        );

        UpdateCustomerCommand command = mapper.toCommand(request);

        assertThat(command.name()).isEqualTo("John Updated");
        assertThat(command.address()).isEqualTo("456 New Avenue");
        assertThat(command.password()).isEqualTo("new-secret");
        assertThat(command.status()).isFalse();
    }

    @Test
    void mapsEntityToResponseWithoutPassword() {
        Customer customer = customer();

        CustomerResponse response = mapper.toResponse(customer);

        assertThat(response.name()).isEqualTo("John Smith");
        assertThat(response.customerId()).isEqualTo("CUS-001");
        assertThat(response.status()).isTrue();
    }

    @Test
    void updatesOnlyNonSensitiveCustomerFields() {
        Customer customer = customer();
        UpdateCustomerCommand command = UpdateCustomerCommand.builder()
                .name("Jane Smith")
                .address("456 New Avenue")
                .customerId("CUS-002")
                .password("raw-password")
                .status(false)
                .build();

        mapper.updateCustomer(command, customer);

        assertThat(customer.getName()).isEqualTo("Jane Smith");
        assertThat(customer.getAddress()).isEqualTo("456 New Avenue");
        assertThat(customer.getCustomerId()).isEqualTo("CUS-002");
        assertThat(customer.getPassword()).isEqualTo("encoded-secret");
        assertThat(customer.getStatus()).isTrue();
        assertThat(customer.getIdentification()).isEqualTo("0102030405");
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
}
