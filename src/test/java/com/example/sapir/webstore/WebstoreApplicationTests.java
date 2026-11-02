package com.example.sapir.webstore;

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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class WebstoreApplicationTests {

    @Autowired ProductService products;
    @Autowired ShoppingCartService carts;
    @Autowired CustomerOrderService orders;
    @Autowired ProductRepository productRepository;
    @Autowired StoreUserRepository userRepository;
    @Autowired ShoppingCartRepository cartRepository;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired CustomerOrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired TransactionTemplate transactions;

    @BeforeEach
    void clearTestDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void productsCanBeCreatedUpdatedAndDeleted() {
        Product created = products.createProduct(product("Mouse", "89.90", 25));
        assertThat(created.getId()).isNotNull();
        assertThat(products.getAllProducts()).hasSize(1);

        products.updateProduct(created.getId(), product("Updated Mouse", "79.50", 20));
        Product updated = products.getProductById(created.getId());
        assertThat(updated.getName()).isEqualTo("Updated Mouse");
        assertThat(updated.getPrice()).isEqualByComparingTo("79.50");
        assertThat(updated.getStockQuantity()).isEqualTo(20);

        products.deleteProduct(created.getId());
        assertThat(products.getAllProducts()).isEmpty();
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
            Product mouse = products.createProduct(product("Mouse", "89.90", 5));
            ShoppingCart cart = carts.createCartForUser(user.getId());
            carts.addProduct(cart.getId(), mouse.getId(), 2);
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
            Product mouse = products.createProduct(product("Mouse", "89.90", 5));
            Product keyboard = products.createProduct(product("Keyboard", "249.00", 1));
            ShoppingCart cart = carts.createCartForUser(user.getId());
            carts.addProduct(cart.getId(), mouse.getId(), 2);
            carts.addProduct(cart.getId(), keyboard.getId(), 2);
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
                int expectedStock = item.getProduct().getName().equals("Mouse") ? 5 : 1;
                assertThat(item.getProduct().getStockQuantity()).isEqualTo(expectedStock);
            }
        });
    }

    private Product product(String name, String price, int quantity) {
        return new Product(name, "A test product", new BigDecimal(price), quantity);
    }
}
