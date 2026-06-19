package com.orderforge.inventory;

import com.orderforge.events.ReleaseInventoryCommand;
import com.orderforge.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "order-created", groupId = "inventory-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        if (!idempotencyService.markIfNew(event.eventId())) return;
        log.info("Received OrderCreatedEvent: orderId={}, items={}",
                event.orderId(), event.items().size());
        inventoryService.reserve(event);
    }

    @KafkaListener(topics = "release-inventory", groupId = "inventory-service")
    public void onReleaseInventory(ReleaseInventoryCommand command) {
        if (!idempotencyService.markIfNew(command.eventId())) return;
        log.warn("Received ReleaseInventoryCommand for order {} ({} items)",
                command.orderId(), command.items().size());
        inventoryService.release(command.orderId(), command.items());
    }
}