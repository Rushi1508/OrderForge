package com.orderforge.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReleaseInventoryCommand(
        UUID eventId,
        UUID orderId,
        List<OrderItem> items,
        LocalDateTime requestedAt
) {}