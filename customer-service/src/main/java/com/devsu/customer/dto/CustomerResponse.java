package com.devsu.customer.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String gender,
        Integer age,
        String identification,
        String address,
        String phone,
        String customerId,
        Boolean status
) {
}
