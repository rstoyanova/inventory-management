package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.exception.ErrorResponse;
import com.inventory.model.StockEntry;
import com.inventory.model.Transaction;
import com.inventory.model.Unit;
import com.inventory.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TransactionController.class)
@AutoConfigureMockMvc
@Testcontainers
public class TransactionControllerIT {

    Logger log = LoggerFactory.getLogger(TransactionControllerIT.class);
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("inventory_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void init() {
        jdbcClient.sql("""
                INSERT INTO transactions (item_name, quantity, unit, price_per_unit, warehouse_name) VALUES
                ('Watermelon', 50, 'KG', 1.20, 'LIDL'),
                ('Potato', 200, 'KG', 0.60, 'LIDL'),
                ('Tomato_ERR', 100, 'KG', 0.90, 'KAUFLAND');
                
                
                INSERT INTO stocks (item_name, quantity, unit, price_per_unit, warehouse_name) VALUES
                ('Watermelon', 50, 'KG', 1.20, 'LIDL'),
                ('Potato', 200, 'KG', 0.60, 'LIDL'),
                ('Tomato_ERR', 100, 'KG', 0.90, 'KAUFLAND');
                """)
                .update();
    }

    @Test
    void testAddTransactions() throws Exception {
        String payload = Files.readString(
                Path.of("src/test/resources/transactions.json"),
                StandardCharsets.UTF_8
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Check if the new transactions are created correctly
        List<Transaction> updatedTransactions = jdbcClient.sql("SELECT * FROM transactions")
                .query((rs, rowNum) -> new Transaction(
                        rs.getLong("id"),
                        rs.getString("item_name"),
                        rs.getDouble("quantity"),
                        Unit.fromValue(rs.getString("unit")),
                        rs.getDouble("price_per_unit"),
                        rs.getString("warehouse_name")
                ))
                .list();

        List<Transaction> tomatoes = updatedTransactions.stream()
                .filter(t-> t.itemName().equals("Tomatoes"))
                .toList();
        assertEquals(2, tomatoes.size());
        Transaction t1 = tomatoes.get(0);
        assertNotNull(t1);
        assertEquals("Tomatoes", t1.itemName());
        assertEquals(10, t1.quantity());
        assertEquals(Unit.KG, t1.unit());
        assertEquals(2.60, t1.pricePerUnit());
        assertEquals("LIDL", t1.warehouseName());

        Transaction t2 = tomatoes.get(1);
        assertNotNull(t2);
        assertEquals("Tomatoes", t2.itemName());
        assertEquals(120, t2.quantity());
        assertEquals(Unit.KG, t2.unit());
        assertEquals(2.60, t2.pricePerUnit());
        assertEquals("LIDL", t2.warehouseName());

        List<Transaction> cheese = updatedTransactions.stream()
                .filter(t-> t.itemName().equals("Cheese"))
                .toList();
        assertEquals(1, cheese.size());
        Transaction t3 = cheese.get(0);
        assertNotNull(t3);
        assertEquals("Cheese", t3.itemName());
        assertEquals(100, t3.quantity());
        assertEquals(Unit.KG, t3.unit());
        assertEquals(22, t3.pricePerUnit());
        assertEquals("KAUFLAND", t3.warehouseName());

        // Check if the stocks table is updated correctly
        Map<String, StockEntry> stockMap = jdbcClient.sql("SELECT * FROM stocks")
                .query((rs, rowNum) -> new StockEntry(
                        rs.getLong("id"),
                        rs.getString("item_name"),
                        rs.getDouble("quantity"),
                        Unit.fromValue(rs.getString("unit")),
                        rs.getDouble("price_per_unit"),
                        rs.getString("warehouse_name")
                ))
                .list()
                .stream()
                .collect(Collectors.toMap(StockEntry::itemName, Function.identity()));

        String str = stockMap.toString();
        log.info(str);

        StockEntry s1 = stockMap.get("Tomatoes");
        assertNotNull(s1);
        assertEquals(130, s1.quantity());
        assertEquals(Unit.KG, s1.unit());
        assertEquals(2.60, s1.pricePerUnit());
        assertEquals("LIDL", s1.warehouseName());

        StockEntry s2 = stockMap.get("Cheese");
        assertNotNull(s2);
        assertEquals(100, s2.quantity());
        assertEquals(Unit.KG, s2.unit());
        assertEquals(22, s2.pricePerUnit());
        assertEquals("KAUFLAND", s2.warehouseName());

    }

    @Test
    void testCreateCorrectionTransactions() throws Exception {
        String payload = Files.readString(
                Path.of("src/test/resources/correction_transactions_1.json"),
                StandardCharsets.UTF_8
        );

        mockMvc.perform(patch("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        // Check if the new transactions are created correctly
        List<Transaction> updatedTransactions = jdbcClient.sql("SELECT * FROM transactions")
                .query((rs, rowNum) -> new Transaction(
                        rs.getLong("id"),
                        rs.getString("item_name"),
                        rs.getDouble("quantity"),
                        Unit.fromValue(rs.getString("unit")),
                        rs.getDouble("price_per_unit"),
                        rs.getString("warehouse_name")
                ))
                .list();

        Map<Long, Transaction> transactionMap = updatedTransactions.stream()
                .collect(Collectors.toMap(Transaction::id, Function.identity()));

        Transaction t1 = transactionMap.get(4L);
        assertNotNull(t1);
        assertEquals("Watermelon", t1.itemName());
        assertEquals(77, t1.quantity());
        assertEquals(Unit.KG, t1.unit());
        assertEquals(1.40, t1.pricePerUnit());
        assertEquals("LIDL", t1.warehouseName());

        Transaction t2 = transactionMap.get(5L);
        assertNotNull(t2);
        assertEquals("Potato", t2.itemName());
        assertEquals(123, t2.quantity());
        assertEquals(Unit.KG, t2.unit());
        assertEquals(0.60, t2.pricePerUnit());
        assertEquals("KAUFLAND", t2.warehouseName());

        Transaction t3 = transactionMap.get(6L);
        assertNotNull(t3);
        assertEquals("Tomato", t3.itemName());
        assertEquals(100, t3.quantity());
        assertEquals(Unit.LB, t3.unit());
        assertEquals(0.90, t3.pricePerUnit());
        assertEquals("KAUFLAND", t3.warehouseName());

        // Check if the stocks table is updated correctly
        Map<Long, StockEntry> stockMap = jdbcClient.sql("SELECT * FROM stocks")
                .query((rs, rowNum) -> new StockEntry(
                        rs.getLong("id"),
                        rs.getString("item_name"),
                        rs.getDouble("quantity"),
                        Unit.fromValue(rs.getString("unit")),
                        rs.getDouble("price_per_unit"),
                        rs.getString("warehouse_name")
                ))
                .list()
                .stream()
                .collect(Collectors.toMap(StockEntry::id, Function.identity()));

        String str = stockMap.toString();
        log.info(str);

        StockEntry s1 = stockMap.get(1L);
        assertNotNull(s1);
        assertEquals("Watermelon", s1.itemName());
        assertEquals(77.0, s1.quantity());
        assertEquals(Unit.KG, s1.unit());
        assertEquals(1.40, s1.pricePerUnit());
        assertEquals("LIDL", s1.warehouseName());

        StockEntry s2 = stockMap.get(2L);
        assertNotNull(s2);
        assertEquals("Potato", s2.itemName());
        assertEquals(123.0, s2.quantity());
        assertEquals(Unit.KG, s2.unit());
        assertEquals(0.60, s2.pricePerUnit());
        assertEquals("KAUFLAND", s2.warehouseName());

        StockEntry s3 = stockMap.get(3L);
        assertNotNull(s3);
        assertEquals("Tomato", s3.itemName());
        assertEquals(100.0, s3.quantity());
        assertEquals(Unit.LB, s3.unit());
        assertEquals(0.90, s3.pricePerUnit());
        assertEquals("KAUFLAND", s3.warehouseName());
    }

    @Test
    void testCorrectionTransactionCreatedNewStock() throws Exception {
        String payload = Files.readString(
                Path.of("src/test/resources/correction_transactions_2.json"),
                StandardCharsets.UTF_8
        );

        mockMvc.perform(patch("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());

        jdbcClient.sql("""
                INSERT INTO transactions (item_name, quantity, unit, price_per_unit, warehouse_name) VALUES
                ('Watermelon', 33, 'KG', 1.20, 'LIDL');
                
                UPDATE stocks
                SET quantity = 83
                WHERE item_name = 'Watermelon'
                  AND price_per_unit = 1.20
                  AND warehouse_name = 'LIDL';
                """)
                .update();

        // Check if the stocks table is updated correctly
        List<StockEntry> stockMap = jdbcClient.sql("SELECT * FROM stocks")
                .query((rs, rowNum) -> new StockEntry(
                        rs.getLong("id"),
                        rs.getString("item_name"),
                        rs.getDouble("quantity"),
                        Unit.fromValue(rs.getString("unit")),
                        rs.getDouble("price_per_unit"),
                        rs.getString("warehouse_name")
                ))
                .list()
                .stream()
                .filter(s -> s.itemName().equals("Watermelon"))
                .toList();

        assertEquals(2, stockMap.size());
        String str = stockMap.toString();
        log.info(str);
        StockEntry stock1 = stockMap.get(0);
        assertNotNull(stock1);
        assertEquals("Watermelon", stock1.itemName());
        assertEquals(20.40, stock1.quantity());
        assertEquals(Unit.KG, stock1.unit());
        assertEquals(2.30, stock1.pricePerUnit());
        assertEquals("LIDL", stock1.warehouseName());

        StockEntry stock2 = stockMap.get(1);
        assertNotNull(stock2);
        assertEquals("Watermelon", stock2.itemName());
        assertEquals(50, stock2.quantity());
        assertEquals(Unit.KG, stock2.unit());
        assertEquals(1.20, stock2.pricePerUnit());
        assertEquals("LIDL", stock2.warehouseName());
    }

    @Test
    void testTransactionNotFoundExceptionHandling() throws Exception {
        String payload = """
        [
            {
                "originalTransactionId": 999,
                "itemName": "watermelon",
                "quantity": 10000.0,
                "unit": "KG",
                "pricePerUnit": 2.0,
                "warehouseName": "LIDL"
            }
        ]
        """;

        String response = mockMvc.perform(patch("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        ErrorResponse error = objectMapper.readValue(response, ErrorResponse.class);

        assertEquals("Transaction Not Found", error.error());
        assertEquals("Transaction with id 999 not found!", error.message());
    }

    @Test
    void testStockNotFoundExceptionHandling() throws Exception {
        jdbcClient.sql("DELETE FROM stocks WHERE item_name = 'Watermelon'").update();

        String payload = """
        [
            {
                "originalTransactionId": 1,
                "itemName": "watermelon",
                "quantity": 10000.0,
                "unit": "KG",
                "pricePerUnit": 2.0,
                "warehouseName": "LIDL"
            }
        ]
        """;

        String response = mockMvc.perform(patch("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        ErrorResponse error = objectMapper.readValue(response, ErrorResponse.class);

        assertEquals("tock Not Found", error.error());
        assertEquals("Transaction with id 999 not found!", error.message());
    }
}
