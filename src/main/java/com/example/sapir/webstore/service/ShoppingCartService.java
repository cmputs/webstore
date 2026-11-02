package com.example.sapir.webstore.service;

import com.example.sapir.webstore.entity.CartItem;
import com.example.sapir.webstore.entity.Product;
import com.example.sapir.webstore.entity.ShoppingCart;
import com.example.sapir.webstore.entity.StoreUser;
import com.example.sapir.webstore.repository.ProductRepository;
import com.example.sapir.webstore.repository.ShoppingCartRepository;
import com.example.sapir.webstore.repository.StoreUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ShoppingCartService {

    private final ShoppingCartRepository cartRepository;
    private final StoreUserRepository userRepository;
    private final ProductRepository productRepository;

    public ShoppingCartService(
            ShoppingCartRepository cartRepository,
            StoreUserRepository userRepository,
            ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ShoppingCart getCartById(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shopping cart not found: " + cartId));
    }

    @Transactional(readOnly = true)
    public ShoppingCart getCartForUser(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shopping cart not found for user: " + userId));
    }

    public ShoppingCart createCartForUser(Long userId) {
        if (cartRepository.findByUserId(userId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This user already has a shopping cart");
        }

        StoreUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));

        return cartRepository.save(new ShoppingCart(user));
    }

    public ShoppingCart addProduct(Long cartId, Long productId, int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        ShoppingCart cart = getCartById(cartId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + productId));

        cart.addItem(new CartItem(product, quantity));
        return cartRepository.save(cart);
    }

    public void removeItem(Long cartId, Long cartItemId) {
        ShoppingCart cart = getCartById(cartId);
        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cart item not found: " + cartItemId));

        cart.removeItem(item);
        cartRepository.save(cart);
    }
}
