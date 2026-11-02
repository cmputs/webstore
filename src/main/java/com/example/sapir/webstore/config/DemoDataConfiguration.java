package com.example.sapir.webstore.config;

import com.example.sapir.webstore.entity.Product;
import com.example.sapir.webstore.repository.ProductRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DemoDataConfiguration {

    @Bean
    @ConditionalOnProperty(name = "webstore.seed-demo-data", havingValue = "true")
    ApplicationRunner sampleProducts(ProductRepository products) {
        return args -> {
            if (products.count() == 0) {
                products.saveAll(List.of(
                        new Product("Mallow Bunny", "A floppy-eared cuddle buddy for slow, cozy mornings.", new BigDecimal("89.90"), 25),
                        new Product("Peachy Pudding Bear", "A squishy little bear with the sweetest, sunny smile.", new BigDecimal("129.00"), 12),
                        new Product("Cloudlet Kitten", "A soft cloud-colored kitty made for naps and daydreams.", new BigDecimal("99.90"), 18),
                        new Product("Strawberry Mochi Pup", "A rosy-cheeked puppy with an extra-squishy hug.", new BigDecimal("119.00"), 8),
                        new Product("Sleepy Star Axolotl", "A dreamy water friend who loves bedtime stories.", new BigDecimal("149.00"), 0)
                ));
            }
        };
    }
}
