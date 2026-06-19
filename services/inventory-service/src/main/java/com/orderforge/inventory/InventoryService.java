package com.orderforge.inventory;

import com.orderforge.events.InventoryReservedEvent;
import com.orderforge.events.InventoryReservationFailedEvent;
import com.orderforge.events.OrderCreatedEvent;
import com.orderforge.events.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockRepository stockRepository;
    private final OutboxService outboxService;

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        boolean ok = tryReserve(event);

        if (ok) {
            InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    event.items(),
                    LocalDateTime.now());
            outboxService.stage(
                    "Order",
                    event.orderId().toString(),
                    "InventoryReservedEvent",
                    "inventory-reserved",
                    "com.orderforge.events.InventoryReservedEvent",
                    reservedEvent);
            log.info("Reservation SUCCESS for order {}: {} items reserved",
                    event.orderId(), event.items().size());
        } else {
            InventoryReservationFailedEvent failedEvent = new InventoryReservationFailedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    "Insufficient stock",
                    LocalDateTime.now());
            outboxService.stage(
                    "Order",
                    event.orderId().toString(),
                    "InventoryReservationFailedEvent",
                    "inventory-reservation-failed",
                    "com.orderforge.events.InventoryReservationFailedEvent",
                    failedEvent);
            log.warn("Reservation FAILED for order {}: insufficient stock", event.orderId());
        }
    }

    private boolean tryReserve(OrderCreatedEvent event) {
        // First pass: check every item has enough stock
        for (OrderItem item : event.items()) {
            Stock stock = stockRepository.findById(item.sku()).orElse(null);
            if (stock == null || stock.getAvailable() < item.quantity()) {
                log.warn("Insufficient stock for sku={} (need {}, have {})",
                        item.sku(), item.quantity(),
                        stock == null ? 0 : stock.getAvailable());
                return false;
            }
        }

        // Second pass: deduct (safe now that all checks passed)
        for (OrderItem item : event.items()) {
            Stock stock = stockRepository.findById(item.sku()).orElseThrow();
            stock.setAvailable(stock.getAvailable() - item.quantity());
            stockRepository.save(stock);
        }
        return true;
    }

    @Transactional
    public void release(java.util.UUID orderId, java.util.List<OrderItem> items) {
        for (OrderItem item : items) {
            Stock stock = stockRepository.findById(item.sku()).orElse(null);
            if (stock == null) {
                log.warn("Cannot release sku={} for order {}: stock row not found",
                        item.sku(), orderId);
                continue;
            }
            stock.setAvailable(stock.getAvailable() + item.quantity());
            stockRepository.save(stock);
        }
        log.info("Released stock for order {}: {} items returned", orderId, items.size());
    }
}