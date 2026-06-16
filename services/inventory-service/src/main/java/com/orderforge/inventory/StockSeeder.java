package com.orderforge.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockSeeder {

    private final StockRepository stockRepository;

    @Bean
    public ApplicationRunner seedStock() {
        return args -> {
            seed("WIDGET-1", 100);
            seed("GADGET-9", 50);
            seed("GIZMO-3", 25);
            log.info("Stock seeded: {} SKUs", stockRepository.count());
        };
    }

    private void seed(String sku, int qty) {
        if (!stockRepository.existsById(sku)) {
            stockRepository.save(new Stock(sku, qty));
        }
    }
}