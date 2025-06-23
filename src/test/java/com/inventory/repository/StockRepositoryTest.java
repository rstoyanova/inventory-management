package com.inventory.repository;

import com.inventory.model.StockEntry;
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
@Import(StockRepository.class)
@Testcontainers
public class StockRepositoryTest {
    private final String itemName = "Strawberry";
    private final String warehouseName = "LIDL";
    private final double price = 5.25;
    private final double quantity = 50.0;
    private final Unit unit = Unit.KG;

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

    StockRepository stockRepository;

    @BeforeEach
    void setup() {
        stockRepository = new StockRepository(jdbcClient);
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcClient.sql("TRUNCATE TABLE stocks, transactions RESTART IDENTITY CASCADE").update();
    }

    @Test
    void testCreateStock() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);

        Optional<StockEntry> result = stockRepository.getByCompositeKey(itemName, warehouseName, price);

        assertThat(result).isPresent();
        StockEntry entry = result.get();
        assertThat(entry.itemName()).isEqualTo(itemName);
        assertThat(entry.quantity()).isEqualTo(quantity);
        assertThat(entry.unit()).isEqualTo(unit);
        assertThat(entry.pricePerUnit()).isEqualTo(price);
        assertThat(entry.warehouseName()).isEqualTo(warehouseName);
    }

    @Test
    void testUpdateStockItemName() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);
        StockEntry entry = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();

        stockRepository.updateStockItemName(entry.id(), "Blueberry");

        Optional<StockEntry> updated = stockRepository.getByCompositeKey("Blueberry", warehouseName, price);
        assertThat(updated).isPresent();
        assertThat(updated.get().itemName()).isEqualTo("Blueberry");
    }

    @Test
    void testUpdateStockQuantity() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);

        StockEntry entry = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();
        stockRepository.updateStockQuantity(entry.id(), 100.0);

        StockEntry updated = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();
        assertThat(updated.quantity()).isEqualTo(100.0);
    }

    @Test
    void testUpdateStockUnit() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);
        StockEntry entry = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();

        stockRepository.updateStockUnit(entry.id(), Unit.LB);

        StockEntry updated = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();
        assertThat(updated.unit()).isEqualTo(Unit.LB);
    }

    @Test
    void testUpdateStockPricePerUnit() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);
        StockEntry entry = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();

        stockRepository.updateStockPricePerUnit(entry.id(), 6.99);

        Optional<StockEntry> updated = stockRepository.getByCompositeKey(itemName, warehouseName, 6.99);
        assertThat(updated).isPresent();
        assertThat(updated.get().pricePerUnit()).isEqualTo(6.99);
    }

    @Test
    void testUpdateStockWarehouseName() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);
        StockEntry entry = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();

        stockRepository.updateStockWarehouseName(entry.id(), "KAUFLAND");

        Optional<StockEntry> updated = stockRepository.getByCompositeKey(itemName, "KAUFLAND", price);
        assertThat(updated).isPresent();
        assertThat(updated.get().warehouseName()).isEqualTo("KAUFLAND");
    }

    @Test
    void testUpsertStock() {
        stockRepository.upsertStock(itemName, 30.0, unit, price, warehouseName);
        stockRepository.upsertStock(itemName, 20.0, unit, price, warehouseName);

        StockEntry result = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();
        assertThat(result.quantity()).isEqualTo(50.0);
    }

    @Test
    void testDeleteStock() {
        stockRepository.createStock(itemName, quantity, unit, price, warehouseName);
        StockEntry entry = stockRepository.getByCompositeKey(itemName, warehouseName, price).orElseThrow();

        stockRepository.deleteStock(entry);

        Optional<StockEntry> result = stockRepository.getByCompositeKey(itemName, warehouseName, price);
        assertThat(result).isNotPresent();
    }


}
