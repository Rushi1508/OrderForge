package com.orderforge.order.service;

import com.orderforge.events.OrderCreatedEvent;
import com.orderforge.order.domain.Order;
import com.orderforge.order.domain.OrderStatus;
import com.orderforge.order.dto.CreateOrderRequest;
import com.orderforge.order.dto.OrderResponse;
import com.orderforge.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        Order order = new Order();
        order.setCustomerId(req.getCustomerId());
        order.setTotalAmount(req.getTotalAmount());

        req.getItems().forEach(i ->
            order.addItem(new com.orderforge.order.domain.OrderItem(i.getSku(), i.getQuantity())));

        Order saved = orderRepository.save(order);

        var eventItems = saved.getItems().stream()
            .map(i -> new com.orderforge.events.OrderItem(i.getSku(), i.getQuantity()))
            .toList();

        kafkaTemplate.send("order-created", saved.getId().toString(),
            new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getTotalAmount(),
                eventItems,
                saved.getCreatedAt()
            ));

        log.info("Order {} created with {} items, OrderCreatedEvent published",
                saved.getId(), eventItems.size());
        return toResponse(saved);
    }

    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} CONFIRMED", orderId);
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = findOrThrow(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} CANCELLED", orderId);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        return toResponse(findOrThrow(id));
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setCustomerId(order.getCustomerId());
        res.setStatus(order.getStatus());
        res.setTotalAmount(order.getTotalAmount());
        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());
        return res;
    }
}