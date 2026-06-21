package com.devsu.customer.service;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.event.CustomerEventFactory;
import com.devsu.customer.event.CustomerEventType;
import com.devsu.customer.exception.BusinessException;
import com.devsu.customer.exception.CustomerNotFoundException;
import com.devsu.customer.exception.ImmutableCustomerIdException;
import com.devsu.customer.mapper.CustomerMapper;
import com.devsu.customer.outbox.OutboxEventRepository;
import com.devsu.customer.repository.CustomerRepository;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerUniquenessValidator uniquenessValidator;
    private final CustomerFactory customerFactory;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomerEventFactory customerEventFactory;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Customer createCustomer(CreateCustomerCommand command) {
        uniquenessValidator.validateForCreation(command.identification(), command.customerId());
        Customer customer = customerRepository.save(customerFactory.create(command));
        saveEvent(customer, CustomerEventType.CUSTOMER_CREATED);
        return customer;
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByCustomerId(String customerId) {
        return findRequiredCustomer(customerId);
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customerRepository.findAll();
    }

    @Transactional
    public Customer updateCustomer(String customerId, UpdateCustomerCommand command) {
        Customer customer = findRequiredCustomer(customerId);
        validateImmutableCustomerId(customer, command.customerId());
        uniquenessValidator.validateForUpdate(customer, command.identification());
        customerMapper.updateCustomer(command, customer);
        updateSensitiveFields(customer, command);
        saveEvent(customer, CustomerEventType.CUSTOMER_UPDATED);
        return customer;
    }

    @Transactional
    public Customer updateCustomerStatus(String customerId, Boolean status) {
        if (status == null) {
            throw new BusinessException("El estado es obligatorio");
        }
        Customer customer = findRequiredCustomer(customerId);
        customer.changeStatus(status);
        saveEvent(customer, CustomerEventType.CUSTOMER_STATUS_CHANGED);
        return customer;
    }

    @Transactional
    public void softDeleteCustomer(String customerId) {
        Customer customer = findRequiredCustomer(customerId);
        customer.deactivate();
        saveEvent(customer, CustomerEventType.CUSTOMER_DELETED);
    }

    private Customer findRequiredCustomer(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private void updateSensitiveFields(Customer customer, UpdateCustomerCommand command) {
        if (hasText(command.password())) {
            customer.changePassword(passwordEncoder.encode(command.password()));
        }
        if (command.status() != null) {
            customer.changeStatus(command.status());
        }
    }

    private void validateImmutableCustomerId(Customer customer, String requestedCustomerId) {
        if (requestedCustomerId != null
                && !Objects.equals(customer.getCustomerId(), requestedCustomerId)) {
            throw new ImmutableCustomerIdException();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void saveEvent(Customer customer, CustomerEventType eventType) {
        outboxEventRepository.save(customerEventFactory.create(customer, eventType));
    }
}
