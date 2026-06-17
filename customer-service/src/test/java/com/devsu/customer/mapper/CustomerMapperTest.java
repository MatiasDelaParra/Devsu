package com.devsu.customer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.dto.UpdateCustomerRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerMapperTest {

    private final CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void mapsCreateRequestToEntity() {
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

        Customer customer = mapper.toEntity(request);

        assertThat(customer.getName()).isEqualTo("John Smith");
        assertThat(customer.getGender()).isEqualTo("MALE");
        assertThat(customer.getAge()).isEqualTo(32);
        assertThat(customer.getIdentification()).isEqualTo("0102030405");
        assertThat(customer.getAddress()).isEqualTo("123 Main Avenue");
        assertThat(customer.getPhone()).isEqualTo("0999999999");
        assertThat(customer.getCustomerId()).isEqualTo("CUS-001");
        assertThat(customer.getPassword()).isEqualTo("secret");
        assertThat(customer.getStatus()).isTrue();
    }

    @Test
    void appliesOnlyProvidedUpdateFields() {
        Customer customer = new Customer();
        customer.setName("John Smith");
        customer.setGender("MALE");
        customer.setAge(32);
        customer.setIdentification("0102030405");
        customer.setAddress("123 Main Avenue");
        customer.setPhone("0999999999");
        customer.setCustomerId("CUS-001");
        customer.setPassword("secret");
        customer.setStatus(true);

        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "John Updated",
                null,
                null,
                null,
                "456 New Avenue",
                null,
                null,
                "new-secret",
                false
        );

        mapper.updateEntity(customer, request);

        assertThat(customer.getName()).isEqualTo("John Updated");
        assertThat(customer.getGender()).isEqualTo("MALE");
        assertThat(customer.getAddress()).isEqualTo("456 New Avenue");
        assertThat(customer.getPassword()).isEqualTo("new-secret");
        assertThat(customer.getStatus()).isFalse();
    }

    @Test
    void mapsEntityToResponseWithoutPassword() {
        Customer customer = new Customer();
        customer.setName("John Smith");
        customer.setGender("MALE");
        customer.setAge(32);
        customer.setIdentification("0102030405");
        customer.setAddress("123 Main Avenue");
        customer.setPhone("0999999999");
        customer.setCustomerId("CUS-001");
        customer.setPassword("secret");
        customer.setStatus(true);

        CustomerResponse response = mapper.toResponse(customer);

        assertThat(response.name()).isEqualTo("John Smith");
        assertThat(response.customerId()).isEqualTo("CUS-001");
        assertThat(response.status()).isTrue();
    }
}
