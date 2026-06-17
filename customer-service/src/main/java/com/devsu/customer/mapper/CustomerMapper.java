package com.devsu.customer.mapper;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.dto.UpdateCustomerRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    Customer toEntity(CreateCustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget Customer customer, UpdateCustomerRequest request);

    CustomerResponse toResponse(Customer customer);
}
