package com.devsu.account.api;

import com.devsu.account.dto.CreateMovementRequest;
import com.devsu.account.dto.MovementResponse;
import com.devsu.account.service.MovementService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;

    @PostMapping
    public ResponseEntity<MovementResponse> createMovement(
            @Valid @RequestBody CreateMovementRequest request
    ) {
        MovementResponse movement = movementService.createMovement(request);
        return ResponseEntity.created(URI.create("/api/movimientos/" + movement.id())).body(movement);
    }

    @GetMapping
    public ResponseEntity<Page<MovementResponse>> listMovements(
            @RequestParam(name = "numeroCuenta", required = false) String accountNumber,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable
    ) {
        return ResponseEntity.ok(movementService.listMovements(accountNumber, pageable));
    }

    @GetMapping("/{movimientoId}")
    public ResponseEntity<MovementResponse> getMovement(
            @PathVariable UUID movimientoId
    ) {
        return ResponseEntity.ok(movementService.getMovement(movimientoId));
    }
}
