package com.orderforge.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotEmpty(message = "At least one item is required")
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank(message = "SKU is required")
        private String sku;

        @Positive(message = "Quantity must be positive")
        private int quantity;
    }
}