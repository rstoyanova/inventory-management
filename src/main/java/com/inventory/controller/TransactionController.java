package com.inventory.controller;

import com.inventory.dto.TransactionPatchRequest;
import com.inventory.dto.TransactionPostRequest;
import com.inventory.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("api/v1/transactions")
@RestController
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("")
    void createTransactions(@Valid @RequestBody List<TransactionPostRequest> transactions) {
        transactions.forEach(transaction -> {
            log.info("Creating transaction ({},{},{},{},{})",
                    transaction.itemName(), transaction.quantity(),
                    transaction.unit(), transaction.pricePerUnit(), transaction.warehouseName());
            transactionService.addTransaction(transaction);
        });
    }


    @ResponseStatus(HttpStatus.ACCEPTED)
    @PatchMapping("")
    void correctTransactions(@Valid @RequestBody List<TransactionPatchRequest> transactions) {
        transactions.forEach(transaction -> {
            log.info("Correction transaction with id: {}", transaction.originalTransactionId());
            transactionService.correctTransaction(transaction);
        });
    }

}
