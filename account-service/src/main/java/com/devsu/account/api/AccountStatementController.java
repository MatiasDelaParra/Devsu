package com.devsu.account.api;

import com.devsu.account.dto.AccountStatementResponse;
import com.devsu.account.service.AccountStatementService;
import com.devsu.account.service.ReportDateRange;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountStatementService accountStatementService;

    @GetMapping
    public ResponseEntity<AccountStatementResponse> generateStatement(
            @RequestParam("fecha") String dateRange,
            @RequestParam("cliente")
            @NotBlank(message = "El cliente es obligatorio")
            String customerId
    ) {
        return ResponseEntity.ok(
                accountStatementService.generate(customerId, ReportDateRange.parse(dateRange))
        );
    }
}
