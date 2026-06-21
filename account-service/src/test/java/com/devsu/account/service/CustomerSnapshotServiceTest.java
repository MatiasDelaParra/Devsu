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
import com.devsu.account.repository.ProcessedCustomerEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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

    @Mock
    private ProcessedCustomerEventRepository processedEventRepository;

    private CustomerSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new CustomerSnapshotService(repository, processedEventRepository);
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
        verify(processedEventRepository).save(
                org.mockito.ArgumentMatchers.any()
        );
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
        when(processedEventRepository.existsById(event.eventId()))
                .thenReturn(false)
                .thenReturn(true);
        when(repository.findByCustomerId("customer-1")).thenReturn(Optional.empty());

        service.apply(event);
        service.apply(event);

        verify(repository, times(1)).save(org.mockito.ArgumentMatchers.any(CustomerSnapshot.class));
        verify(processedEventRepository, times(1)).save(
                org.mockito.ArgumentMatchers.any()
        );
    }

    private CustomerEvent event(CustomerEventType type, Boolean status) {
        return new CustomerEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
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
