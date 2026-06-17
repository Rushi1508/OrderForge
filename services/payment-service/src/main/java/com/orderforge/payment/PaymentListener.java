package com.orderforge.payment;

import com.orderforge.events.PaymentProcessedEvent;
import com.orderforge.events.PaymentDeclinedEvent;
import java.math.BigDecimal;
import com.orderforge.events.ProcessPaymentCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "process-payment", groupId = "payment-service")
    public void onProcessPayment(ProcessPaymentCommand command) {
        log.info("Received ProcessPaymentCommand for order {} (amount={})",
                command.orderId(), command.amount());

        // Simulated payment: decline if amount exceeds threshold, otherwise approve
        BigDecimal declineThreshold = new BigDecimal("1000");
        if (command.amount().compareTo(declineThreshold) > 0) {
            log.warn("Payment DECLINED for order {}: amount {} exceeds threshold {}",
                    command.orderId(), command.amount(), declineThreshold);
            PaymentDeclinedEvent declined = new PaymentDeclinedEvent(
                    command.orderId(),
                    command.customerId(),
                    command.amount(),
                    "Amount exceeds limit",
                    LocalDateTime.now());
            kafkaTemplate.send("payment-declined", command.orderId().toString(), declined);
            log.warn("Published PaymentDeclinedEvent for order {}", command.orderId());
        } else {
            String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Payment SUCCESS for order {}: paymentId={}", command.orderId(), paymentId);
            PaymentProcessedEvent event = new PaymentProcessedEvent(
                    command.orderId(),
                    command.customerId(),
                    command.amount(),
                    paymentId,
                    LocalDateTime.now());
            kafkaTemplate.send("payment-processed", command.orderId().toString(), event);
            log.info("Published PaymentProcessedEvent for order {}", command.orderId());
        }
    }
}