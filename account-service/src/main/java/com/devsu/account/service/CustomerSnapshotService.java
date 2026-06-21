package com.devsu.account.service;

import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.domain.ProcessedCustomerEvent;
import com.devsu.account.event.CustomerEvent;
import com.devsu.account.exception.InvalidCustomerEventException;
import com.devsu.account.repository.CustomerSnapshotRepository;
import com.devsu.account.repository.ProcessedCustomerEventRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerSnapshotService {

    private final CustomerSnapshotRepository repository;
    private final ProcessedCustomerEventRepository processedEventRepository;

    @Transactional
    public void apply(CustomerEvent event) {
        validateCommonFields(event);

        if (isAlreadyProcessed(event)) {
            return;
        }

        Optional<CustomerSnapshot> existing = repository.findByCustomerId(event.customerId());

        if (isStaleEvent(existing, event)) {
            markProcessed(event);
            return;
        }

        applyEvent(existing, event);
        markProcessed(event);
    }

    private boolean isAlreadyProcessed(CustomerEvent event) {
        return processedEventRepository.existsById(event.eventId());
    }

    private boolean isStaleEvent(Optional<CustomerSnapshot> existing, CustomerEvent event) {
        return existing
                .map(snapshot -> snapshot.isNewerThan(event.occurredAt()))
                .orElse(false);
    }

    private void applyEvent(Optional<CustomerSnapshot> existing, CustomerEvent event) {
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
        validateStatus(event);

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

        if (hasSnapshotFields(event)) {
            repository.save(deletedSnapshot(event));
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

    private CustomerSnapshot deletedSnapshot(CustomerEvent event) {
        return CustomerSnapshot.builder()
                .customerId(event.customerId())
                .name(event.name())
                .identification(event.identification())
                .status(false)
                .createdAt(event.occurredAt())
                .updatedAt(event.occurredAt())
                .build();
    }

    private void markProcessed(CustomerEvent event) {
        processedEventRepository.save(ProcessedCustomerEvent.builder()
                .eventId(event.eventId())
                .customerId(event.customerId())
                .processedAt(Instant.now())
                .build());
    }

    private void validateCommonFields(CustomerEvent event) {
        require(event != null, "Customer event is required");
        require(event.type() != null, "Customer event type is required");
        require(event.eventId() != null, "Customer event id is required");
        require(StringUtils.hasText(event.customerId()), "Customer id is required");
        require(event.occurredAt() != null, "Customer event occurredAt is required");
    }

    private void validateSnapshotFields(CustomerEvent event) {
        require(StringUtils.hasText(event.name()), "Customer name is required");
        require(StringUtils.hasText(event.identification()), "Customer identification is required");
        validateStatus(event);
    }

    private void validateStatus(CustomerEvent event) {
        require(event.status() != null, "Customer status is required");
    }

    private boolean hasSnapshotFields(CustomerEvent event) {
        return StringUtils.hasText(event.name())
                && StringUtils.hasText(event.identification());
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidCustomerEventException(message);
        }
    }
}