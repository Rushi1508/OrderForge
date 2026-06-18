package com.orderforge.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        BigDecimal amount,
        String paymentId,
        LocalDateTime processedAt
) {}