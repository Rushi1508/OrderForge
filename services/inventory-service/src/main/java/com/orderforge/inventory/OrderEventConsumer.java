package com.orderforge.inventory;
import com.orderforge.events.InventoryReservedEvent;
import com.orderforge.events.InventoryReservationFailedEvent;
import com.orderforge.events.ReleaseInventoryCommand;
import com.orderforge.events.OrderCreatedEvent;
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
                    UUID.randomUUID(),
                    event.orderId(),
                    event.items(),
                    LocalDateTime.now());
            kafkaTemplate.send("inventory-reserved", event.orderId().toString(), reservedEvent);
            log.info("Published InventoryReservedEvent for order {}", event.orderId());
        }
        else {
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    "Insufficient stock",
                    LocalDateTime.now());
            kafkaTemplate.send("inventory-reservation-failed", event.orderId().toString(), failedEvent);
            log.warn("Published InventoryReservationFailedEvent for order {}", event.orderId());
        }
    }
    @KafkaListener(topics = "release-inventory", groupId = "inventory-service")
    public void onReleaseInventory(ReleaseInventoryCommand command) {
        log.warn("Received ReleaseInventoryCommand for order {} ({} items)",
                command.orderId(), command.items().size());
        inventoryService.release(command.orderId(), command.items());
    }
}