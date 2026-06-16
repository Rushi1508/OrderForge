package com.orderforge.inventory;

import com.orderforge.events.InventoryReservedEvent;
import com.orderforge.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order-created", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, items={}",
                event.orderId(), event.items().size());

        boolean reserved = inventoryService.reserve(event);

        if (reserved) {
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    event.orderId(),
                    event.items(),
                    LocalDateTime.now());
            kafkaTemplate.send("inventory-reserved", event.orderId().toString(), reservedEvent);
            log.info("Published InventoryReservedEvent for order {}", event.orderId());
        }
        // Failure path (publish InventoryReservationFailed) comes in Phase 3
    }
}