package com.devsu.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String gender,
        @NotNull @Min(0) Integer age,
        @NotBlank @Size(max = 50) String identification,
        @NotBlank @Size(max = 200) String address,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Size(max = 50) String customerId,
        @NotBlank @Size(max = 255) String password,
        @NotNull Boolean status
) {
}
