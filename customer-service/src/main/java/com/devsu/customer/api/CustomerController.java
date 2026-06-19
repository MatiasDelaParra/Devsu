package com.devsu.customer.api;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.dto.CreateCustomerRequest;
import com.devsu.customer.dto.CustomerResponse;
import com.devsu.customer.dto.UpdateCustomerRequest;
import com.devsu.customer.dto.UpdateCustomerStatusRequest;
import com.devsu.customer.mapper.CustomerMapper;
import com.devsu.customer.service.CustomerService;
import com.devsu.customer.service.command.CreateCustomerCommand;
import com.devsu.customer.service.command.UpdateCustomerCommand;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CreateCustomerCommand command = customerMapper.toCommand(request);
        Customer createdCustomer = customerService.createCustomer(command);

        return ResponseEntity
                .created(URI.create("/api/clientes/" + createdCustomer.getCustomerId()))
                .body(customerMapper.toResponse(createdCustomer));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> listCustomers() {
        List<CustomerResponse> customers = customerService.listCustomers()
                .stream()
                .map(customerMapper::toResponse)
                .toList();

        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable("clienteId") String customerId) {
        Customer customer = customerService.getCustomerByCustomerId(customerId);
        return ResponseEntity.ok(customerMapper.toResponse(customer));
    }

    @PutMapping("/{clienteId}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable("clienteId") String customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        UpdateCustomerCommand command = customerMapper.toCommand(request);
        Customer updatedCustomer = customerService.updateCustomer(customerId, command);
        return ResponseEntity.ok(customerMapper.toResponse(updatedCustomer));
    }

    @PatchMapping("/{clienteId}/estado")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(
            @PathVariable("clienteId") String customerId,
            @Valid @RequestBody UpdateCustomerStatusRequest request
    ) {
        Customer updatedCustomer = customerService.updateCustomerStatus(customerId, request.status());
        return ResponseEntity.ok(customerMapper.toResponse(updatedCustomer));
    }

    @DeleteMapping("/{clienteId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("clienteId") String customerId) {
        customerService.softDeleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
