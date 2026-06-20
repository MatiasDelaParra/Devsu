package com.devsu.account.api;

import com.devsu.account.exception.AccountNotFoundException;
import com.devsu.account.exception.BusinessException;
import com.devsu.account.exception.CustomerSnapshotNotFoundException;
import com.devsu.account.exception.DuplicateAccountException;
import com.devsu.account.exception.MovementNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> API_FIELD_NAMES = Map.ofEntries(
            Map.entry("value", "valor"),
            Map.entry("accountNumber", "numeroCuenta"),
            Map.entry("accountType", "tipoCuenta"),
            Map.entry("initialBalance", "saldoInicial"),
            Map.entry("status", "estado"),
            Map.entry("customerId", "clienteId")
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
            AccountNotFoundException.class,
            CustomerSnapshotNotFoundException.class,
            MovementNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateAccountException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessError(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido o contiene campos no permitidos",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation while processing {}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad de datos",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected error while processing {}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno inesperado",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                translateStatus(status),
                message,
                request.getRequestURI()
        ));
    }

    private String formatFieldError(FieldError error) {
        return API_FIELD_NAMES.getOrDefault(error.getField(), error.getField())
                + ": "
                + error.getDefaultMessage();
    }

    private String translateStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Solicitud Incorrecta";
            case NOT_FOUND -> "No Encontrado";
            case CONFLICT -> "Conflicto";
            case INTERNAL_SERVER_ERROR -> "Error Interno del Servidor";
            default -> "Error HTTP " + status.value();
        };
    }
}
