package com.devsu.customer.service;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.exception.DuplicateCustomerException;
import com.devsu.customer.repository.CustomerRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerUniquenessValidator {

    private final CustomerRepository customerRepository;

    public void validateForCreation(String identification, String customerId) {
        if (customerRepository.existsByIdentification(identification)) {
            throw DuplicateCustomerException.forIdentification(identification);
        }
        if (customerRepository.existsByCustomerId(customerId)) {
            throw DuplicateCustomerException.forCustomerId(customerId);
        }
    }

    public void validateForUpdate(Customer customer, String identification) {
        UUID customerPrimaryKey = customer.getId();

        if (hasChanged(identification, customer.getIdentification())
                && customerRepository.existsByIdentificationAndIdNot(identification, customerPrimaryKey)) {
            throw DuplicateCustomerException.forIdentification(identification);
        }
    }

    private boolean hasChanged(String candidate, String currentValue) {
        return !Objects.equals(candidate, currentValue);
    }
}
