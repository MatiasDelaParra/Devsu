package com.devsu.customer.service;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.service.command.CreateCustomerCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerFactory {

    private final PasswordEncoder passwordEncoder;

    public Customer create(CreateCustomerCommand command) {
        return Customer.builder()
                .name(command.name())
                .gender(command.gender())
                .age(command.age())
                .identification(command.identification())
                .address(command.address())
                .phone(command.phone())
                .customerId(command.customerId())
                .password(passwordEncoder.encode(command.password()))
                .status(command.status() == null ? Boolean.TRUE : command.status())
                .build();
    }
}
