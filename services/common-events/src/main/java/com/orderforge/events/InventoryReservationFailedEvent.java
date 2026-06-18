package com.orderforge.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        LocalDateTime failedAt
) {}