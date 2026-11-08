package com.example.sapir.webstore;

import com.example.sapir.webstore.dto.RegisterRequest;
import com.example.sapir.webstore.entity.*;
import com.example.sapir.webstore.repository.*;
import com.example.sapir.webstore.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class WebstoreApplicationTests {

    @Autowired ProductService products;
    @Autowired CategoryService categories;
    @Autowired ShoppingCartService carts;
    @Autowired CustomerOrderService orders;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired StoreUserRepository userRepository;
    @Autowired ShoppingCartRepository cartRepository;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired CustomerOrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired TransactionTemplate transactions;
    @Autowired StoreUserService storeUsers;
    @Autowired StoreUserDetailsService storeUserDetails;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearTestDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void registrationStoresOnlyABcryptPasswordHashAndEnablesLoginLookup() {
        storeUsers.register(new RegisterRequest("student", "student@example.com", "DemoPass123"));

        StoreUser saved = userRepository.findByUsername("student").orElseThrow();
        assertThat(saved.getPasswordHash()).hasSize(60).startsWith("$2");
        assertThat(saved.getPasswordHash()).isNotEqualTo("DemoPass123");
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(passwordEncoder.matches("DemoPass123", saved.getPasswordHash())).isTrue();
        assertThat(storeUserDetails.loadUserByUsername("student"))
                .satisfies(user -> {
                    assertThat(user.getUsername()).isEqualTo("student");
                    assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
                });
    }

    @Test
    void registrationRejectsDuplicateUserDetails() {
        storeUsers.register(new RegisterRequest("student", "student@example.com", "DemoPass123"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> storeUsers.register(new RegisterRequest("student", "other@example.com", "AnotherPass123")));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void usersWithoutPasswordHashesCannotLogIn() {
        userRepository.save(new StoreUser("legacy", "legacy@example.com"));

        assertThrows(UsernameNotFoundException.class, () -> storeUserDetails.loadUserByUsername("legacy"));
    }

    @Test
    void productsCanBeCreatedUpdatedAndDeleted() {
        Product created = products.createProduct(product("Mallow Bunny", "89.90", 25));
        assertThat(created.getId()).isNotNull();
        assertThat(products.getAllProducts()).hasSize(1);

        products.updateProduct(created.getId(), product("Updated Mallow Bunny", "79.50", 20));
        Product updated = products.getProductById(created.getId());
        assertThat(updated.getName()).isEqualTo("Updated Mallow Bunny");
        assertThat(updated.getPrice()).isEqualByComparingTo("79.50");
        assertThat(updated.getStockQuantity()).isEqualTo(20);

        products.deleteProduct(created.getId());
        assertThat(products.getAllProducts()).isEmpty();
    }

    @Test
    void categoriesCanBeAssignedFilteredAndDeletedWithoutDeletingProducts() {
        Category woodland = categories.createCategory(new Category("Woodland Buddies", "Forest plushies"));
        Category treats = categories.createCategory(new Category("Sweet Treats", "Dessert plushies"));

        ResponseStatusException duplicateError = assertThrows(ResponseStatusException.class,
                () -> categories.createCategory(new Category("woodland buddies", "Duplicate name")));
        assertThat(duplicateError.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Category renamed = categories.updateCategory(
                woodland.getId(), new Category("Forest Friends", "Updated forest plushies"));
        assertThat(renamed.getName()).isEqualTo("Forest Friends");
        assertThat(renamed.getDescription()).isEqualTo("Updated forest plushies");

        ResponseStatusException duplicateUpdateError = assertThrows(ResponseStatusException.class,
                () -> categories.updateCategory(treats.getId(), new Category("Forest Friends", "Duplicate name")));
        assertThat(duplicateUpdateError.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Product bunnyRequest = product("Mallow Bunny", "89.90", 25);
        bunnyRequest.setCategory(new Category());
        bunnyRequest.getCategory().setId(woodland.getId());
        Product bunny = products.createProduct(bunnyRequest);

        Product uncategorizedKitten = products.createProduct(product("Cloudlet Kitten", "99.90", 18));

        assertThat(products.getProductsByCategory(woodland.getId()))
                .extracting(Product::getName)
                .containsExactly("Mallow Bunny");
        assertThat(products.getUncategorizedProducts())
                .extracting(Product::getName)
                .containsExactly("Cloudlet Kitten");

        Product moveRequest = product("Mallow Bunny", "89.90", 25);
        moveRequest.setCategory(new Category());
        moveRequest.getCategory().setId(treats.getId());
        Product moved = products.updateProduct(bunny.getId(), moveRequest);
        assertThat(moved.getCategory()).isNotNull();
        assertThat(moved.getCategory().getId()).isEqualTo(treats.getId());

        categories.deleteCategory(treats.getId());
        Product afterCategoryDelete = products.getProductById(bunny.getId());
        assertThat(afterCategoryDelete.getCategory()).isNull();
        assertThat(products.getUncategorizedProducts()).hasSize(2);
    }

    @Test
    void missingProductReturnsNotFound() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> products.getProductById(Long.MAX_VALUE));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aUserCannotHaveTwoShoppingCarts() {
        StoreUser user = userRepository.save(new StoreUser("student", "student@example.com"));
        carts.createCartForUser(user.getId());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> carts.createCartForUser(user.getId()));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cartRepository.count()).isEqualTo(1);
    }

    @Test
    void addingZeroQuantityIsRejected() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> carts.addProduct(1L, 1L, 0));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anEmptyCartCannotBecomeAnOrder() {
        StoreUser user = userRepository.save(new StoreUser("student", "student@example.com"));
        Long cartId = carts.createCartForUser(user.getId()).getId();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orders.createOrderFromCart(cartId));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void checkoutReducesStockClearsTheCartAndPreservesThePurchasePrice() {
        Long cartId = transactions.execute(status -> {
            StoreUser user = userRepository.save(new StoreUser("student", "student@example.com"));
            Product bunny = products.createProduct(product("Mallow Bunny", "89.90", 5));
            ShoppingCart cart = carts.createCartForUser(user.getId());
            carts.addProduct(cart.getId(), bunny.getId(), 2);
            return cart.getId();
        });

        Long orderId = orders.createOrderFromCart(cartId).getId();

        transactions.executeWithoutResult(status -> {
            CustomerOrder order = orders.getOrderById(orderId);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getItems()).hasSize(1);
            OrderItem item = order.getItems().getFirst();
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("89.90");
            assertThat(item.getProduct().getStockQuantity()).isEqualTo(3);
            assertThat(carts.getCartById(cartId).getItems()).isEmpty();

            item.getProduct().setPrice(new BigDecimal("99.90"));
        });

        transactions.executeWithoutResult(status -> {
            OrderItem item = orders.getOrderById(orderId).getItems().getFirst();
            assertThat(item.getProduct().getPrice()).isEqualByComparingTo("99.90");
            assertThat(item.getPriceAtPurchase()).isEqualByComparingTo("89.90");
        });
        assertThat(cartItemRepository.count()).isZero();
    }

    @Test
    void failedCheckoutRollsBackAllStockChangesAndKeepsTheCart() {
        Long cartId = transactions.execute(status -> {
            StoreUser user = userRepository.save(new StoreUser("student", "student@example.com"));
            Product bunny = products.createProduct(product("Mallow Bunny", "89.90", 5));
            Product bear = products.createProduct(product("Peachy Pudding Bear", "129.00", 1));
            ShoppingCart cart = carts.createCartForUser(user.getId());
            carts.addProduct(cart.getId(), bunny.getId(), 2);
            carts.addProduct(cart.getId(), bear.getId(), 2);
            return cart.getId();
        });

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> orders.createOrderFromCart(cartId));
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(orderRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();

        transactions.executeWithoutResult(status -> {
            ShoppingCart cart = carts.getCartById(cartId);
            assertThat(cart.getItems()).hasSize(2);
            for (CartItem item : cart.getItems()) {
                int expectedStock = item.getProduct().getName().equals("Mallow Bunny") ? 5 : 1;
                assertThat(item.getProduct().getStockQuantity()).isEqualTo(expectedStock);
            }
        });
    }

    private Product product(String name, String price, int quantity) {
        return new Product(name, "A soft plushie friend", new BigDecimal(price), quantity);
    }
}
