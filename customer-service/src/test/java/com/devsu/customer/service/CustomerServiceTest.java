package com.devsu.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.event.CustomerEventFactory;
import com.devsu.customer.event.CustomerEventType;
import com.devsu.customer.exception.BusinessException;
import com.devsu.customer.exception.CustomerNotFoundException;
import com.devsu.customer.mapper.CustomerMapper;
import com.devsu.customer.outbox.OutboxEvent;
import com.devsu.customer.outbox.OutboxEventRepository;
import com.devsu.customer.outbox.OutboxEventStatus;
import com.devsu.customer.repository.CustomerRepository;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerUniquenessValidator uniquenessValidator;

    @Mock
    private CustomerFactory customerFactory;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomerEventFactory customerEventFactory;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createsCustomerAfterValidatingUniqueness() {
        CreateCustomerCommand command = createCommand();
        Customer customer = customer();
        OutboxEvent event = outboxEvent(CustomerEventType.CUSTOMER_CREATED);

        when(customerFactory.create(command)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerEventFactory.create(customer, CustomerEventType.CUSTOMER_CREATED)).thenReturn(event);

        Customer createdCustomer = customerService.createCustomer(command);

        assertThat(createdCustomer).isSameAs(customer);
        verify(uniquenessValidator).validateForCreation("0102030405", "CUS-001");
        verify(customerFactory).create(command);
        verify(customerRepository).save(customer);
        verify(outboxEventRepository).save(event);
    }

    @Test
    void throwsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findByCustomerId("CUS-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerByCustomerId("CUS-404"))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void softDeletesManagedCustomerWithoutExplicitSave() {
        Customer customer = customer();
        OutboxEvent event = outboxEvent(CustomerEventType.CUSTOMER_DELETED);
        when(customerRepository.findByCustomerId("CUS-001")).thenReturn(Optional.of(customer));
        when(customerEventFactory.create(customer, CustomerEventType.CUSTOMER_DELETED)).thenReturn(event);

        customerService.softDeleteCustomer("CUS-001");

        assertThat(customer.getStatus()).isFalse();
        verify(outboxEventRepository).save(event);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void updatesManagedCustomerAndEncodesNewPassword() {
        Customer customer = customer();
        UpdateCustomerCommand command = updateCommand();
        OutboxEvent event = outboxEvent(CustomerEventType.CUSTOMER_UPDATED);

        when(customerRepository.findByCustomerId("CUS-001")).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("new-secret")).thenReturn("new-encoded-secret");
        when(customerEventFactory.create(customer, CustomerEventType.CUSTOMER_UPDATED)).thenReturn(event);

        Customer updatedCustomer = customerService.updateCustomer("CUS-001", command);

        assertThat(updatedCustomer).isSameAs(customer);
        assertThat(customer.getPassword()).isEqualTo("new-encoded-secret");
        assertThat(customer.getStatus()).isFalse();
        verify(uniquenessValidator).validateForUpdate(customer, "9999999999", "CUS-002");
        verify(customerMapper).updateCustomer(command, customer);
        verify(outboxEventRepository).save(event);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void changesStatusAndStoresStatusChangedEvent() {
        Customer customer = customer();
        OutboxEvent event = outboxEvent(CustomerEventType.CUSTOMER_STATUS_CHANGED);
        when(customerRepository.findByCustomerId("CUS-001")).thenReturn(Optional.of(customer));
        when(customerEventFactory.create(customer, CustomerEventType.CUSTOMER_STATUS_CHANGED)).thenReturn(event);

        Customer updatedCustomer = customerService.updateCustomerStatus("CUS-001", false);

        assertThat(updatedCustomer.getStatus()).isFalse();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void rejectsNullStatusBeforeLoadingCustomer() {
        assertThatThrownBy(() -> customerService.updateCustomerStatus("CUS-001", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El estado es obligatorio");

        verify(customerRepository, never()).findByCustomerId(any());
    }

    private Customer customer() {
        return Customer.builder()
                .name("John Smith")
                .gender("MALE")
                .age(32)
                .identification("0102030405")
                .address("123 Main Avenue")
                .phone("0999999999")
                .customerId("CUS-001")
                .password("encoded-secret")
                .status(true)
                .build();
    }

    private CreateCustomerCommand createCommand() {
        return CreateCustomerCommand.builder()
                .name("John Smith")
                .gender("MALE")
                .age(32)
                .identification("0102030405")
                .address("123 Main Avenue")
                .phone("0999999999")
                .customerId("CUS-001")
                .password("secret")
                .build();
    }

    private UpdateCustomerCommand updateCommand() {
        return UpdateCustomerCommand.builder()
                .name("Jane Smith")
                .gender("FEMALE")
                .age(31)
                .identification("9999999999")
                .address("456 New Avenue")
                .phone("0988888888")
                .customerId("CUS-002")
                .password("new-secret")
                .status(false)
                .build();
    }

    private OutboxEvent outboxEvent(CustomerEventType eventType) {
        return OutboxEvent.builder()
                .aggregateType("CUSTOMER")
                .aggregateId("CUS-001")
                .eventType(eventType.name())
                .payload("{}")
                .status(OutboxEventStatus.PENDING)
                .createdAt(java.time.Instant.parse("2026-06-18T10:00:00Z"))
                .build();
    }
}
