package com.devsu.account.service;

import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.event.CustomerEvent;
import com.devsu.account.exception.InvalidCustomerEventException;
import com.devsu.account.repository.CustomerSnapshotRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerSnapshotService {

    private final CustomerSnapshotRepository repository;

    @Transactional
    public void apply(CustomerEvent event) {
        validateCommonFields(event);

        Optional<CustomerSnapshot> existing = repository.findByCustomerId(event.customerId());
        if (existing.isPresent() && existing.get().isNewerThan(event.occurredAt())) {
            return;
        }

        switch (event.type()) {
            case CUSTOMER_CREATED, CUSTOMER_UPDATED -> upsertFullSnapshot(existing, event);
            case CUSTOMER_STATUS_CHANGED -> applyStatusChange(existing, event);
            case CUSTOMER_DELETED -> applyDeletion(existing, event);
        }
    }

    private void upsertFullSnapshot(Optional<CustomerSnapshot> existing, CustomerEvent event) {
        validateSnapshotFields(event);
        if (existing.isPresent()) {
            existing.get().update(
                    event.name(),
                    event.identification(),
                    event.status(),
                    event.occurredAt()
            );
            return;
        }
        repository.save(newSnapshot(event));
    }

    private void applyStatusChange(Optional<CustomerSnapshot> existing, CustomerEvent event) {
        if (event.status() == null) {
            throw new InvalidCustomerEventException("Customer status is required");
        }
        if (existing.isPresent()) {
            existing.get().changeStatus(event.status(), event.occurredAt());
            return;
        }
        validateSnapshotFields(event);
        repository.save(newSnapshot(event));
    }

    private void applyDeletion(Optional<CustomerSnapshot> existing, CustomerEvent event) {
        if (existing.isPresent()) {
            existing.get().changeStatus(false, event.occurredAt());
            return;
        }
        if (StringUtils.hasText(event.name()) && StringUtils.hasText(event.identification())) {
            repository.save(CustomerSnapshot.builder()
                    .customerId(event.customerId())
                    .name(event.name())
                    .identification(event.identification())
                    .status(false)
                    .createdAt(event.occurredAt())
                    .updatedAt(event.occurredAt())
                    .build());
        }
    }

    private CustomerSnapshot newSnapshot(CustomerEvent event) {
        return CustomerSnapshot.builder()
                .customerId(event.customerId())
                .name(event.name())
                .identification(event.identification())
                .status(event.status())
                .createdAt(event.occurredAt())
                .updatedAt(event.occurredAt())
                .build();
    }

    private void validateCommonFields(CustomerEvent event) {
        if (event == null || event.type() == null) {
            throw new InvalidCustomerEventException("Customer event type is required");
        }
        if (!StringUtils.hasText(event.customerId())) {
            throw new InvalidCustomerEventException("Customer id is required");
        }
        if (event.occurredAt() == null) {
            throw new InvalidCustomerEventException("Customer event occurredAt is required");
        }
    }

    private void validateSnapshotFields(CustomerEvent event) {
        if (!StringUtils.hasText(event.name())) {
            throw new InvalidCustomerEventException("Customer name is required");
        }
        if (!StringUtils.hasText(event.identification())) {
            throw new InvalidCustomerEventException("Customer identification is required");
        }
        if (event.status() == null) {
            throw new InvalidCustomerEventException("Customer status is required");
        }
    }
}
