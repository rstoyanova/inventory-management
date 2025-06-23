package com.inventory.repository;

import com.inventory.model.Transaction;
import com.inventory.model.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);
    private final JdbcClient jdbcClient;

    public TransactionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Transaction> getById(Long id) {
        log.info("Getting transaction with id: [{}] ...", id);

        return jdbcClient.sql("""
                    SELECT *
                    FROM transactions
                    WHERE id = :id
                    """)
                .param("id", id)
                .query(Transaction.class)
                .optional();
    }

    public void createTransaction(String itemName, Double quantity, Unit unit, Double pricePerUnit, String warehouseName) {
        log.info("Creating transaction for item: [{}, {}, {}, {}, {}] ...",
                itemName, quantity, unit.toString(), pricePerUnit, warehouseName);

        jdbcClient.sql("""
                        INSERT INTO transactions (item_name, quantity, unit, price_per_unit, warehouse_name)
                        VALUES (:item_name, :quantity, :unit, :price_per_unit, :warehouse_name)
                        """)
                .param("item_name", itemName)
                .param("quantity", quantity)
                .param("unit", unit.toString())
                .param("price_per_unit", pricePerUnit)
                .param("warehouse_name", warehouseName)
                .update();
    }

}
