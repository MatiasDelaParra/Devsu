package com.devsu.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.account.domain.CustomerSnapshot;
import com.devsu.account.event.CustomerEvent;
import com.devsu.account.event.CustomerEventType;
import com.devsu.account.repository.CustomerSnapshotRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerSnapshotServiceTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-06-20T12:00:00Z");

    @Mock
    private CustomerSnapshotRepository repository;

    private CustomerSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new CustomerSnapshotService(repository);
    }

    @Test
    void createsSnapshotFromCustomerCreated() {
        CustomerEvent event = event(CustomerEventType.CUSTOMER_CREATED, true);
        when(repository.findByCustomerId("customer-1")).thenReturn(Optional.empty());

        service.apply(event);

        ArgumentCaptor<CustomerSnapshot> snapshotCaptor =
                ArgumentCaptor.forClass(CustomerSnapshot.class);
        verify(repository).save(snapshotCaptor.capture());
        CustomerSnapshot snapshot = snapshotCaptor.getValue();
        assertThat(snapshot.getCustomerId()).isEqualTo("customer-1");
        assertThat(snapshot.getName()).isEqualTo("Jane Doe");
        assertThat(snapshot.getIdentification()).isEqualTo("1234567890");
        assertThat(snapshot.getStatus()).isTrue();
        assertThat(snapshot.getCreatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(snapshot.getUpdatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void updatesExistingSnapshot() {
        CustomerSnapshot snapshot = snapshot("Old Name", true, OCCURRED_AT.minusSeconds(60));
        CustomerEvent event = event(CustomerEventType.CUSTOMER_UPDATED, true);
        when(repository.findByCustomerId("customer-1")).thenReturn(Optional.of(snapshot));

        service.apply(event);

        assertThat(snapshot.getName()).isEqualTo("Jane Doe");
        assertThat(snapshot.getIdentification()).isEqualTo("1234567890");
        assertThat(snapshot.getStatus()).isTrue();
        assertThat(snapshot.getUpdatedAt()).isEqualTo(OCCURRED_AT);
        verify(repository, never()).save(snapshot);
    }

    @Test
    void handlesCustomerDeletedAsSoftDelete() {
        CustomerSnapshot snapshot = snapshot("Jane Doe", true, OCCURRED_AT.minusSeconds(60));
        CustomerEvent event = event(CustomerEventType.CUSTOMER_DELETED, true);
        when(repository.findByCustomerId("customer-1")).thenReturn(Optional.of(snapshot));

        service.apply(event);

        assertThat(snapshot.getStatus()).isFalse();
        assertThat(snapshot.getUpdatedAt()).isEqualTo(OCCURRED_AT);
        verify(repository, never()).delete(snapshot);
    }

    @Test
    void processingSameEventTwiceDoesNotCreateDuplicateSnapshot() {
        CustomerEvent event = event(CustomerEventType.CUSTOMER_CREATED, true);
        CustomerSnapshot persisted = snapshot("Jane Doe", true, OCCURRED_AT);
        when(repository.findByCustomerId("customer-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));

        service.apply(event);
        service.apply(event);

        verify(repository, times(1)).save(org.mockito.ArgumentMatchers.any(CustomerSnapshot.class));
    }

    private CustomerEvent event(CustomerEventType type, Boolean status) {
        return new CustomerEvent(
                type,
                "customer-1",
                "Jane Doe",
                "1234567890",
                status,
                OCCURRED_AT
        );
    }

    private CustomerSnapshot snapshot(String name, Boolean status, Instant updatedAt) {
        return CustomerSnapshot.builder()
                .customerId("customer-1")
                .name(name)
                .identification("old-identification")
                .status(status)
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .build();
    }
}
