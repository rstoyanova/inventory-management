package com.inventory.repository;

import com.inventory.model.StockEntry;
import com.inventory.model.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StockRepository {

    private static final Logger log = LoggerFactory.getLogger(StockRepository.class);
    private final JdbcClient jdbcClient;


    public StockRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<StockEntry> getAll() {
        log.info("Getting all stocks...");

        return jdbcClient.sql("SELECT * FROM stocks")
                .query(StockEntry.class)
                .list();
    }

    public Optional<StockEntry> getByCompositeKey(String itemName, String warehouseName, Double pricePerUnit) {
        log.info("Getting stock entry for [{}, {}, {}] ...", itemName, warehouseName, pricePerUnit);

        return jdbcClient.sql("""
                SELECT * FROM stocks
                WHERE item_name=:item_name AND warehouse_name=:warehouse_name AND price_per_unit=:price_per_unit
                """)
                 .param("item_name", itemName)
                 .param("warehouse_name", warehouseName)
                 .param("price_per_unit", pricePerUnit)
                .query(StockEntry.class)
                .optional();
    }

    public void createStock(String itemName, Double quantity, Unit unit, Double pricePerUnit, String warehouseName) {
        log.info("Creating stock entry for item: [{}] ...", itemName);

        jdbcClient.sql("""
                INSERT INTO stocks (item_name, quantity, unit, price_per_unit, warehouse_name)
                VALUES (:item_name, :quantity, :unit, :price_per_unit, :warehouse_name)
                """)
                .param("item_name", itemName)
                .param("quantity", quantity)
                .param("unit", unit.toString())
                .param("price_per_unit", pricePerUnit)
                .param("warehouse_name", warehouseName)
                .update();
    }

    public void updateStockItemName(Long id, String newItemName) {
        log.info("Updating item name for stock with id [{}] to [{}]...", id, newItemName);

        jdbcClient.sql("""
                UPDATE stocks
                SET item_name=:new_name
                WHERE id = :id
                """)
                .param("id", id)
                .param("new_name", newItemName)
                .update();
    }

    public void updateStockQuantity(Long id, Double newQuantity) {
        log.info("Updating quantity for stock with id [{}] to [{}]...", id, newQuantity);

        jdbcClient.sql("""
                UPDATE stocks
                SET quantity=:quantity
                WHERE id = :id
                """)
                .param("id", id)
                .param("quantity", newQuantity)
                .update();
    }

    public void updateStockUnit(Long id, Unit newUnit) {
        log.info("Updating unit for stock with id [{}] to [{}]...", id, newUnit);

        jdbcClient.sql("""
                UPDATE stocks
                SET unit=:new_unit
                WHERE id = :id
                """)
                .param("id", id)
                .param("new_unit", newUnit.toString())
                .update();
    }

    public void updateStockPricePerUnit(Long id, Double newPrice) {
        log.info("Updating price per unit for stock with id [{}] to [{}]...", id, newPrice);

        jdbcClient.sql("""
                UPDATE stocks
                SET price_per_unit=:new_price
                WHERE id = :id
                """)
                .param("id", id)
                .param("new_price", newPrice)
                .update();
    }

    public void updateStockWarehouseName(Long id, String newWarehouse) {
        log.info("Updating warehouse name for stock with id [{}] to [{}]...", id, newWarehouse);

        jdbcClient.sql("""
                UPDATE stocks
                SET warehouse_name=:new_warehouse
                WHERE id = :id
                """)
                .param("id", id)
                .param("new_warehouse", newWarehouse)
                .update();
    }

    public void deleteStock(StockEntry stockEntry) {
        log.info("Deleting stock [{}]...", stockEntry.itemName());

        jdbcClient.sql("""
                    DELETE FROM stocks
                    WHERE item_name = :item AND price_per_unit = :price AND warehouse_name = :warehouse
                """)
                .param("item", stockEntry.itemName())
                .param("price", stockEntry.pricePerUnit())
                .param("warehouse", stockEntry.warehouseName())
                .update();
    }

    public void upsertStock(String itemName, Double quantity, Unit unit, Double pricePerUnit, String warehouseName) {
        log.info("Upserting stock [{}]...", itemName);

        jdbcClient.sql("""
                INSERT INTO stocks (item_name, quantity, unit, price_per_unit, warehouse_name)
                VALUES (:item_name, :quantity, :unit, :price_per_unit, :warehouse_name)
                ON CONFLICT (item_name, warehouse_name, price_per_unit)
                DO UPDATE SET quantity = stocks.quantity + EXCLUDED.quantity;
                """)
                .param("item_name", itemName)
                .param("quantity", quantity)
                .param("unit", unit.toString())
                .param("price_per_unit", pricePerUnit)
                .param("warehouse_name", warehouseName)
                .update();
    }


}
