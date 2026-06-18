package com.devsu.customer.service;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.exception.BusinessException;
import com.devsu.customer.exception.CustomerNotFoundException;
import com.devsu.customer.mapper.CustomerMapper;
import com.devsu.customer.repository.CustomerRepository;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import java.util.List;
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

    @Transactional
    public Customer createCustomer(CreateCustomerCommand command) {
        uniquenessValidator.validateForCreation(command.identification(), command.customerId());
        return customerRepository.save(customerFactory.create(command));
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
        uniquenessValidator.validateForUpdate(customer, command.identification(), command.customerId());
        customerMapper.updateCustomer(command, customer);
        updateSensitiveFields(customer, command);
        return customer;
    }

    @Transactional
    public Customer updateCustomerStatus(String customerId, Boolean status) {
        if (status == null) {
            throw new BusinessException("El estado es obligatorio");
        }
        Customer customer = findRequiredCustomer(customerId);
        customer.changeStatus(status);
        return customer;
    }

    @Transactional
    public void softDeleteCustomer(String customerId) {
        findRequiredCustomer(customerId).deactivate();
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
