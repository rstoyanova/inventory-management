package com.inventory.service;

import com.inventory.dto.TransactionPatchRequest;
import com.inventory.dto.TransactionPostRequest;
import com.inventory.exception.StockNotFoundException;
import com.inventory.exception.TransactionNotFoundException;
import com.inventory.model.StockEntry;
import com.inventory.model.Transaction;
import com.inventory.repository.StockRepository;
import com.inventory.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final StockRepository stockRepository;

    public TransactionService(TransactionRepository transactionRepository, StockRepository stockRepository) {
        this.transactionRepository = transactionRepository;
        this.stockRepository = stockRepository;
    }

    public void addTransaction(TransactionPostRequest transactionRequestDTO) {
        Transaction transaction = mapTransactionCreationDtoToTransaction(transactionRequestDTO);
        transactionRepository.createTransaction(
                transaction.itemName(),
                transaction.quantity(),
                transaction.unit(),
                transaction.pricePerUnit(),
                transaction.warehouseName());
        stockRepository.upsertStock(
                transaction.itemName(),
                transaction.quantity(),
                transaction.unit(),
                transaction.pricePerUnit(),
                transaction.warehouseName());
    }

    public void correctTransaction(TransactionPatchRequest correction) {
        Long originalTransactionId = correction.originalTransactionId();
        Transaction correctTransaction = mapTransactionCorrectionDtoToTransaction(correction);
        Transaction originalTransaction = transactionRepository.getById(originalTransactionId).orElseThrow(
                () -> new TransactionNotFoundException(originalTransactionId)
        );
        log.info("Original transaction [{}, {}, {}, {}, {}]", originalTransaction.itemName(), originalTransaction.quantity(), originalTransaction.unit().toString(),
                originalTransaction.pricePerUnit(), originalTransaction.warehouseName());
        transactionRepository.createTransaction(
                    correctTransaction.itemName(),
                    correctTransaction.quantity(),
                    correctTransaction.unit(),
                    correctTransaction.pricePerUnit(),
                    correctTransaction.warehouseName());

        StockEntry stockEntryToFix = stockRepository
                .getByCompositeKey(originalTransaction.itemName(), originalTransaction.warehouseName(),originalTransaction.pricePerUnit())
                .orElseThrow(() -> new StockNotFoundException(originalTransaction.itemName(),
                        originalTransaction.warehouseName(), originalTransaction.pricePerUnit()));

        double newQuantity = stockEntryToFix.quantity() - originalTransaction.quantity();
        if (newQuantity == 0) {
            if (!stockEntryToFix.itemName().equals(correctTransaction.itemName())) {
                stockRepository.updateStockItemName(stockEntryToFix.id(), correctTransaction.itemName());
            }
            if (!stockEntryToFix.quantity().equals(correctTransaction.quantity())) {
                stockRepository.updateStockQuantity(stockEntryToFix.id(), correctTransaction.quantity());
            }
            if (!stockEntryToFix.unit().equals(correctTransaction.unit())) {
                stockRepository.updateStockUnit(stockEntryToFix.id(), correctTransaction.unit());
            }
            if (!Objects.equals(stockEntryToFix.pricePerUnit(), correctTransaction.pricePerUnit())) {
                stockRepository.updateStockPricePerUnit(stockEntryToFix.id(), correctTransaction.pricePerUnit());
            }
            if (!stockEntryToFix.warehouseName().equals(correctTransaction.warehouseName())) {
                stockRepository.updateStockWarehouseName(stockEntryToFix.id(), correctTransaction.warehouseName());
            }
        } else {
            stockRepository.createStock(
                    correctTransaction.itemName(),
                    correctTransaction.quantity(),
                    correctTransaction.unit(),
                    correctTransaction.pricePerUnit(),
                    correctTransaction.warehouseName());
            newQuantity = stockEntryToFix.quantity() - originalTransaction.quantity();
            stockRepository.updateStockQuantity(stockEntryToFix.id(), newQuantity);
        }
    }

    private Transaction mapTransactionCreationDtoToTransaction(TransactionPostRequest dto) {
        return new Transaction(
                null,
                dto.itemName(),
                dto.quantity(),
                dto.unit(),
                dto.pricePerUnit(),
                dto.warehouseName()
        );
    }

    private Transaction mapTransactionCorrectionDtoToTransaction(TransactionPatchRequest dto) {
        return new Transaction(
                null,
                dto.itemName(),
                dto.quantity(),
                dto.unit(),
                dto.pricePerUnit(),
                dto.warehouseName()
        );
    }
}
