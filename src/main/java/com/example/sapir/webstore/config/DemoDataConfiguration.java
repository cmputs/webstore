package com.example.sapir.webstore.config;

import com.example.sapir.webstore.entity.Category;
import com.example.sapir.webstore.entity.Product;
import com.example.sapir.webstore.repository.CategoryRepository;
import com.example.sapir.webstore.repository.ProductRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class DemoDataConfiguration {

    private static final Set<String> LEGACY_COMPUTER_PRODUCTS = Set.of(
            "Wireless Mouse",
            "Mechanical Keyboard",
            "USB-C Hub",
            "Laptop Stand",
            "Noise-Cancelling Headphones"
    );

    private static final Map<String, Product> LEGACY_TO_PLUSHIE = Map.of(
            "Wireless Mouse", new Product("Mallow Bunny", "A floppy-eared cuddle buddy for slow, cozy mornings.", new BigDecimal("89.90"), 25),
            "Mechanical Keyboard", new Product("Peachy Pudding Bear", "A squishy little bear with the sweetest, sunny smile.", new BigDecimal("129.00"), 12),
            "USB-C Hub", new Product("Cloudlet Kitten", "A soft cloud-colored kitty made for naps and daydreams.", new BigDecimal("99.90"), 18),
            "Laptop Stand", new Product("Strawberry Mochi Pup", "A rosy-cheeked puppy with an extra-squishy hug.", new BigDecimal("119.00"), 8),
            "Noise-Cancelling Headphones", new Product("Sleepy Star Axolotl", "A dreamy water friend who loves bedtime stories.", new BigDecimal("149.00"), 0)
    );

    private static final Map<String, String> PLUSHIE_DEFAULT_CATEGORY = Map.of(
            "Mallow Bunny", "Woodland Buddies",
            "Peachy Pudding Bear", "Sweet Treats",
            "Cloudlet Kitten", "Cloud & Naptime",
            "Strawberry Mochi Pup", "Sweet Treats",
            "Sleepy Star Axolotl", "Ocean & Starlight"
    );

    @Bean
    @ConditionalOnProperty(name = "webstore.seed-demo-data", havingValue = "true")
    ApplicationRunner sampleProducts(ProductRepository products, CategoryRepository categories) {
        return args -> {
            if (categories.count() == 0) {
                categories.saveAll(List.of(
                        new Category("Woodland Buddies", "Floppy-eared bunnies, forest bears, and cozy woodland pals."),
                        new Category("Sweet Treats", "Pastel dessert-inspired plushies as sweet as pudding and mochi."),
                        new Category("Cloud & Naptime", "Extra-soft, dreamy companions made for naps and bedtime stories."),
                        new Category("Ocean & Starlight", "Magical sea creatures and starry night cuddle friends."),
                        new Category("Pocket Pals", "Mini travel-sized plushies to carry in your bag everywhere.")
                ));
            }

            List<Product> existing = products.findAll();
            boolean updatedLegacy = false;
            for (Product product : existing) {
                if (LEGACY_COMPUTER_PRODUCTS.contains(product.getName())) {
                    Product plushie = LEGACY_TO_PLUSHIE.get(product.getName());
                    product.setName(plushie.getName());
                    product.setDescription(plushie.getDescription());
                    product.setPrice(plushie.getPrice());
                    product.setStockQuantity(plushie.getStockQuantity());
                    products.save(product);
                    updatedLegacy = true;
                }
            }

            if (existing.isEmpty() && !updatedLegacy) {
                existing = products.saveAll(List.of(
                        new Product("Mallow Bunny", "A floppy-eared cuddle buddy for slow, cozy mornings.", new BigDecimal("89.90"), 25),
                        new Product("Peachy Pudding Bear", "A squishy little bear with the sweetest, sunny smile.", new BigDecimal("129.00"), 12),
                        new Product("Cloudlet Kitten", "A soft cloud-colored kitty made for naps and daydreams.", new BigDecimal("99.90"), 18),
                        new Product("Strawberry Mochi Pup", "A rosy-cheeked puppy with an extra-squishy hug.", new BigDecimal("119.00"), 8),
                        new Product("Sleepy Star Axolotl", "A dreamy water friend who loves bedtime stories.", new BigDecimal("149.00"), 0)
                ));
            }

            // Assign default categories to the sample plushies only when all products are currently uncategorized
            boolean anyCategorized = existing.stream().anyMatch(product -> product.getCategory() != null);
            if (!anyCategorized) {
                for (Product product : existing) {
                    String categoryName = PLUSHIE_DEFAULT_CATEGORY.get(product.getName());
                    if (categoryName != null) {
                        categories.findByNameIgnoreCase(categoryName).ifPresent(category -> {
                            product.setCategory(category);
                            products.save(product);
                        });
                    }
                }
            }
        };
    }
}
