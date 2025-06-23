package com.inventory.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse("Internal Server Error", ex.getMessage(), LocalDateTime.now()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFound(StockNotFoundException ex) {
        log.warn("Stock not found: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse("Stock Not Found!", ex.getMessage(), LocalDateTime.now()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());

        return new ResponseEntity<>(
                new ErrorResponse("Transaction Not Found!", ex.getMessage(), LocalDateTime.now()),
                HttpStatus.NOT_FOUND
        );
    }

    record ErrorResponse(String error, String message, LocalDateTime timestamp) {}
}
