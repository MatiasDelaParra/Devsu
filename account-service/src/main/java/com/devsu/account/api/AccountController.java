package com.devsu.account.api;

import com.devsu.account.dto.AccountResponse;
import com.devsu.account.dto.CreateAccountRequest;
import com.devsu.account.dto.UpdateAccountRequest;
import com.devsu.account.service.AccountService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse account = accountService.createAccount(request);
        return ResponseEntity
                .created(URI.create("/api/cuentas/" + account.accountNumber()))
                .body(account);
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> listAccounts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable("numeroCuenta") String accountNumber
    ) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @PutMapping("/{numeroCuenta}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable("numeroCuenta") String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return ResponseEntity.ok(accountService.updateAccount(accountNumber, request));
    }
}
