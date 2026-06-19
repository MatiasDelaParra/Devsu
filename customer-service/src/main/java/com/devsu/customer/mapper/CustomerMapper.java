package com.devsu.customer.mapper;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.dto.UpdateCustomerRequest;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CreateCustomerCommand toCommand(CreateCustomerRequest request);

    UpdateCustomerCommand toCommand(UpdateCustomerRequest request);

    CustomerResponse toResponse(Customer customer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateCustomer(UpdateCustomerCommand command, @MappingTarget Customer customer);
}
