package com.orderforge.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID orderId,
        List<OrderItem> reservedItems,
        LocalDateTime reservedAt
) {
}