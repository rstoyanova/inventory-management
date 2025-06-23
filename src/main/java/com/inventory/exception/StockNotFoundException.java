package com.inventory.exception;

public class StockNotFoundException extends RuntimeException {
  public StockNotFoundException(String itemName, String warehouseName, Double pricePerUnit) {
    super(String.format("Stock [%s, %s, %s] was not found!", itemName, warehouseName, pricePerUnit));
  }
}
