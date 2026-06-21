package com.devsu.account.repository;

import com.devsu.account.domain.ProcessedCustomerEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedCustomerEventRepository
        extends JpaRepository<ProcessedCustomerEvent, UUID> {
}
