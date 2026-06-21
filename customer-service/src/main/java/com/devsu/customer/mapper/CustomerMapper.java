package com.devsu.customer.mapper;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.dto.UpdateCustomerRequest;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class CustomerMapper {

    public CreateCustomerCommand toCommand(CreateCustomerRequest request) {
        return CreateCustomerCommand.builder()
                .name(request.name())
                .gender(request.gender())
                .age(request.age())
                .identification(request.identification())
                .address(request.address())
                .phone(request.phone())
                .customerId(request.customerId())
                .password(request.password())
                .status(request.status())
                .build();
    }

    public UpdateCustomerCommand toCommand(UpdateCustomerRequest request) {
        return UpdateCustomerCommand.builder()
                .name(request.name())
                .gender(request.gender())
                .age(request.age())
                .identification(request.identification())
                .address(request.address())
                .phone(request.phone())
                .customerId(request.customerId())
                .password(request.password())
                .status(request.status())
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getGender(),
                customer.getAge(),
                customer.getIdentification(),
                customer.getAddress(),
                customer.getPhone(),
                customer.getCustomerId(),
                customer.getStatus()
        );
    }

    public void updateCustomer(UpdateCustomerCommand command, Customer customer) {
        updateIfPresent(command.name(), customer::setName);
        updateIfPresent(command.gender(), customer::setGender);
        updateIfPresent(command.age(), customer::setAge);
        updateIfPresent(command.identification(), customer::setIdentification);
        updateIfPresent(command.address(), customer::setAddress);
        updateIfPresent(command.phone(), customer::setPhone);
    }

    private <T> void updateIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
