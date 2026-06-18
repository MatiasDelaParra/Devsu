package com.devsu.customer.service.command;

import lombok.Builder;

@Builder
public record CreateCustomerCommand(
        String name,
        String gender,
        Integer age,
        String identification,
        String address,
        String phone,
        String customerId,
        String password,
        Boolean status
) {
}
