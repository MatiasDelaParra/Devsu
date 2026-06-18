package com.devsu.customer.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.exception.DuplicateCustomerException;
import com.devsu.customer.repository.CustomerRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerUniquenessValidatorTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerUniquenessValidator validator;

    @Test
    void rejectsDuplicateIdentificationOnCreation() {
        when(customerRepository.existsByIdentification("0102030405")).thenReturn(true);

        assertThatThrownBy(() -> validator.validateForCreation("0102030405", "CUS-001"))
                .isInstanceOf(DuplicateCustomerException.class);

        verify(customerRepository, never()).existsByCustomerId("CUS-001");
    }

    @Test
    void rejectsDuplicateCustomerIdOnCreation() {
        when(customerRepository.existsByCustomerId("CUS-001")).thenReturn(true);

        assertThatThrownBy(() -> validator.validateForCreation("0102030405", "CUS-001"))
                .isInstanceOf(DuplicateCustomerException.class);
    }

    @Test
    void checksOnlyChangedFieldsOnUpdate() {
        UUID id = UUID.randomUUID();
        Customer customer = customer(id);

        validator.validateForUpdate(customer, "0102030405", "CUS-001");

        verify(customerRepository, never()).existsByIdentificationAndIdNot("0102030405", id);
        verify(customerRepository, never()).existsByCustomerIdAndIdNot("CUS-001", id);
    }

    @Test
    void rejectsChangedDuplicateIdentificationOnUpdate() {
        UUID id = UUID.randomUUID();
        Customer customer = customer(id);
        when(customerRepository.existsByIdentificationAndIdNot("9999999999", id)).thenReturn(true);

        assertThatThrownBy(() -> validator.validateForUpdate(customer, "9999999999", null))
                .isInstanceOf(DuplicateCustomerException.class);
    }

    private Customer customer(UUID id) {
        return Customer.builder()
                .id(id)
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
