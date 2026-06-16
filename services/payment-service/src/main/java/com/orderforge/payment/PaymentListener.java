package com.orderforge.payment;

import com.orderforge.events.PaymentProcessedEvent;
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

        // Simulated payment processing — always succeeds for now
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