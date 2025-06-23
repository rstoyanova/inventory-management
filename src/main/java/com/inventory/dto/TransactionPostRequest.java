package com.inventory.dto;

import com.inventory.model.Unit;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionPostRequest(
        @NotEmpty String itemName,
        @Positive Double quantity,
        @NotNull Unit unit,
        @Positive Double pricePerUnit,
        @NotEmpty String warehouseName
) {}
