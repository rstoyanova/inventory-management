package com.inventory.model;


public record Transaction(Long id,
                          String itemName,
                          Double quantity,
                          Unit unit,
                          Double pricePerUnit,
                          String warehouseName) {
}
