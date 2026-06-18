package com.orderforge.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDeclinedEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        BigDecimal amount,
        String reason,
        LocalDateTime declinedAt
) {}