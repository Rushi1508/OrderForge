package com.orderforge.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter @Setter @NoArgsConstructor
public class ProcessedEvent {
    @Id
    private UUID eventId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }
}