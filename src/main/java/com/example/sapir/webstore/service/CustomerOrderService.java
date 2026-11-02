package com.example.sapir.webstore.service;

import com.example.sapir.webstore.entity.CartItem;
import com.example.sapir.webstore.entity.CustomerOrder;
import com.example.sapir.webstore.entity.OrderItem;
import com.example.sapir.webstore.entity.OrderStatus;
import com.example.sapir.webstore.entity.Product;
import com.example.sapir.webstore.entity.ShoppingCart;
import com.example.sapir.webstore.repository.CustomerOrderRepository;
import com.example.sapir.webstore.repository.OrderItemRepository;
import com.example.sapir.webstore.repository.ShoppingCartRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShoppingCartRepository cartRepository;

    public CustomerOrderService(
            CustomerOrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ShoppingCartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CustomerOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> getOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getItemsForOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found: " + orderId);
        }
        return orderItemRepository.findByCustomerOrderId(orderId);
    }

    public CustomerOrder createOrderFromCart(Long cartId) {
        ShoppingCart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shopping cart not found: " + cartId));

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot create an order from an empty cart");
        }

        CustomerOrder order = new CustomerOrder(cart.getUser());
        List<CartItem> cartItems = new ArrayList<>(cart.getItems());

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Not enough stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            order.addItem(new OrderItem(
                    product,
                    cartItem.getQuantity(),
                    product.getPrice()));
            cart.removeItem(cartItem);
        }

        CustomerOrder savedOrder = orderRepository.save(order);
        cartRepository.save(cart);
        return savedOrder;
    }

    public CustomerOrder changeOrderStatus(Long orderId, OrderStatus newStatus) {
        CustomerOrder order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}
