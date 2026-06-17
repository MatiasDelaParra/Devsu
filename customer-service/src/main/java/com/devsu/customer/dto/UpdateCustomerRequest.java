package com.devsu.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @Size(max = 100) String name,
        @Size(max = 20) String gender,
        @Min(0) Integer age,
        @Size(max = 50) String identification,
        @Size(max = 200) String address,
        @Size(max = 30) String phone,
        @Size(max = 50) String customerId,
        @Size(max = 255) String password,
        Boolean status
) {
}
