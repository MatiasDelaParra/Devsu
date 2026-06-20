package com.devsu.account.repository;

import com.devsu.account.domain.CustomerSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSnapshotRepository extends JpaRepository<CustomerSnapshot, UUID> {

    Optional<CustomerSnapshot> findByCustomerId(String customerId);

    List<CustomerSnapshot> findByCustomerIdIn(Collection<String> customerIds);

    boolean existsByCustomerId(String customerId);
}
