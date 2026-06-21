package com.devsu.customer.repository;

import com.devsu.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByIdentification(String identification);

    boolean existsByCustomerId(String customerId);

    boolean existsByIdentification(String identification);

    boolean existsByIdentificationAndIdNot(String identification, UUID id);
}
