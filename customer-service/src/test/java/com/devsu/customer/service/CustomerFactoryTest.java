package com.devsu.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.service.command.CreateCustomerCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerFactoryTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerFactory customerFactory;

    @Test
    void buildsCustomerWithEncodedPasswordAndDefaultStatus() {
        CreateCustomerCommand command = command(null);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        Customer customer = customerFactory.create(command);

        assertThat(customer.getName()).isEqualTo("John Smith");
        assertThat(customer.getCustomerId()).isEqualTo("CUS-001");
        assertThat(customer.getPassword()).isEqualTo("encoded-secret");
        assertThat(customer.getStatus()).isTrue();
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void preservesExplicitStatus() {
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        Customer customer = customerFactory.create(command(false));

        assertThat(customer.getStatus()).isFalse();
    }

    private CreateCustomerCommand command(Boolean status) {
        return CreateCustomerCommand.builder()
                .name("John Smith")
                .gender("MALE")
                .age(32)
                .identification("0102030405")
                .address("123 Main Avenue")
                .phone("0999999999")
                .customerId("CUS-001")
                .password("secret")
                .status(status)
                .build();
    }
}
