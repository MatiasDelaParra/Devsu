package com.devsu.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "customers",
        schema = "customer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customers_customer_id", columnNames = "customer_id")
        }
)
@PrimaryKeyJoinColumn(name = "person_id")
public class Customer extends Person {

    @NotBlank
    @Size(max = 50)
    @Column(name = "customer_id", nullable = false, updatable = false, length = 50)
    private String customerId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @NotNull
    @Column(name = "status", nullable = false)
    private Boolean status;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeStatus(boolean status) {
        this.status = status;
    }

    public void deactivate() {
        changeStatus(false);
    }
}
