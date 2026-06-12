package com.orderforge.events;

public record OrderItem(
        String sku,
        int quantity
) {
}