package com.inventory.repository;

import com.inventory.model.Transaction;
import com.inventory.model.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TransactionRepository.class)
@Testcontainers
class TransactionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("inventory_test")
            .withUsername("postgres")
            .withPassword("secret");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JdbcClient jdbcClient;

    TransactionRepository transactionRepository;

    @BeforeEach
    void setup() {
        transactionRepository = new TransactionRepository(jdbcClient);
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcClient.sql("TRUNCATE TABLE stocks, transactions RESTART IDENTITY CASCADE").update();
    }

    @Test
    void testCreateTransaction() {
        String itemName = "Onion";
        double quantity = 50.0;
        Unit unit = Unit.KG;
        double price = 3.2;
        String warehouse = "LIDL";

        transactionRepository.createTransaction(itemName, quantity, unit, price, warehouse);

        Optional<Transaction> result = jdbcClient
                .sql("SELECT * FROM transactions WHERE item_name = :item_name")
                .param("item_name", itemName)
                .query(Transaction.class)
                .optional();

        assertThat(result).isPresent();
        assertThat(result.get().itemName()).isEqualTo(itemName);
        assertThat(result.get().quantity()).isEqualTo(quantity);
        assertThat(result.get().unit()).isEqualTo(unit);
        assertThat(result.get().pricePerUnit()).isEqualTo(price);
        assertThat(result.get().warehouseName()).isEqualTo(warehouse);
    }
}
