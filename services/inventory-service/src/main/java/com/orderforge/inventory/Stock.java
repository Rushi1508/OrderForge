package com.orderforge.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock")
@Getter @Setter @NoArgsConstructor
public class Stock {

    @Id
    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int available;

    public Stock(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }
}