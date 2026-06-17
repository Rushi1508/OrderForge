package com.orderforge.order.service;

import com.orderforge.events.InventoryReservedEvent;
import com.orderforge.events.InventoryReservationFailedEvent;
import com.orderforge.events.PaymentDeclinedEvent;
import com.orderforge.events.ReleaseInventoryCommand;
import com.orderforge.events.OrderItem;
import com.orderforge.order.domain.Order;
import java.util.List;
import com.orderforge.events.PaymentProcessedEvent;
import com.orderforge.events.ProcessPaymentCommand;
import com.orderforge.order.domain.Order;
import com.orderforge.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaListener {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "inventory-reserved", groupId = "order-service")
    public void onInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for order {}", event.orderId());

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found; cannot proceed to payment", event.orderId());
            return;
        }

        ProcessPaymentCommand command = new ProcessPaymentCommand(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                LocalDateTime.now());

        kafkaTemplate.send("process-payment", order.getId().toString(), command);
        log.info("Published ProcessPaymentCommand for order {} (amount={})",
                order.getId(), order.getTotalAmount());
    }

    @KafkaListener(topics = "payment-processed", groupId = "order-service")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent for order {} (paymentId={})",
                event.orderId(), event.paymentId());

        orderService.confirmOrder(event.orderId());
        log.info("Order {} marked CONFIRMED - saga complete", event.orderId());
    }

    @KafkaListener(topics = "inventory-reservation-failed", groupId = "order-service")
    public void onReservationFailed(InventoryReservationFailedEvent event) {
        log.warn("Received InventoryReservationFailedEvent for order {}: {}",
                event.orderId(), event.reason());
        orderService.cancelOrder(event.orderId());
        log.warn("Order {} marked CANCELLED - reservation failed", event.orderId());
    }

    @KafkaListener(topics = "payment-declined", groupId = "order-service")
    @org.springframework.transaction.annotation.Transactional
    public void onPaymentDeclined(PaymentDeclinedEvent event) {
        log.warn("Received PaymentDeclinedEvent for order {}: {}",
                event.orderId(), event.reason());

        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Order {} not found; cannot compensate", event.orderId());
            return;
        }

        // Map persisted line items -> event items for the release command
        List<OrderItem> itemsToRelease = order.getItems().stream()
                .map(i -> new OrderItem(i.getSku(), i.getQuantity()))
                .toList();

        ReleaseInventoryCommand release = new ReleaseInventoryCommand(
                event.orderId(),
                itemsToRelease,
                LocalDateTime.now());
        kafkaTemplate.send("release-inventory", event.orderId().toString(), release);
        log.warn("Published ReleaseInventoryCommand for order {} ({} items)",
                event.orderId(), itemsToRelease.size());

        orderService.cancelOrder(event.orderId());
        log.warn("Order {} marked CANCELLED - payment declined", event.orderId());
    }
}