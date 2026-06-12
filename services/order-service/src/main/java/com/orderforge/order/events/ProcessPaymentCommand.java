package com.orderforge.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand {
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
}