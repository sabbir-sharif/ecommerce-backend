package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartItemResponse;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    // 1. Get the authenticated user's cart
    @Override
    @Transactional
    public CartResponse getMyCart() {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        return buildCartResponse(cart);
    }

    // 2. Add a product to the cart
    @Override
    @Transactional
    public CartResponse addItem(AddToCartRequest request) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(
                request.getProductId()
        ).orElseThrow(() ->
                new ResourceNotFoundException("Product not found.")
        );

        validateActiveProduct(product);

        Inventory inventory = getInventory(product.getId());

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int currentQuantity = existingItem == null
                ? 0
                : existingItem.getQuantity();

        int newQuantity;

        try {
            newQuantity = Math.addExact(
                    currentQuantity,
                    request.getQuantity()
            );
        } catch (ArithmeticException exception) {
            throw new ConflictException(
                    "The requested cart quantity is too large."
            );
        }

        validateStock(inventory, newQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(newQuantity);

            cartItemRepository.save(cartItem);
        }

        return buildCartResponse(cart);
    }

    // 3. Update a cart item's quantity
    @Override
    @Transactional
    public CartResponse updateItem(
            int cartItemId,
            UpdateCartItemRequest request) {

        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = getOwnedCartItem(cart, cartItemId);

        Product product = cartItem.getProduct();

        validateActiveProduct(product);

        Inventory inventory = getInventory(product.getId());

        validateStock(inventory, request.getQuantity());

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return buildCartResponse(cart);
    }

    // 4. Remove one cart item
    @Override
    @Transactional
    public CartResponse removeItem(int cartItemId) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = getOwnedCartItem(cart, cartItemId);

        cartItemRepository.delete(cartItem);

        return buildCartResponse(cart);
    }

    // 5. Clear the cart
    @Override
    @Transactional
    public CartResponse clearCart() {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        cartItemRepository.deleteByCartId(cart.getId());

        return buildCartResponse(cart);
    }

    // Find the authenticated user using the JWT authentication
    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {

            throw new AccessDeniedException(
                    "Authentication is required to access the cart."
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated user was not found."
                        )
                );
    }

    // Find an existing cart or create one
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);

                    return cartRepository.save(cart);
                });
    }

    // Find a cart item and verify that it belongs to this user
    private CartItem getOwnedCartItem(Cart cart, int cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found."
                        )
                );

        if (cartItem.getCart().getId() != cart.getId()) {
            throw new ResourceNotFoundException(
                    "Cart item not found."
            );
        }

        return cartItem;
    }

    // Ensure that only active products can be used in the cart
    private void validateActiveProduct(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ConflictException(
                    "This product is not available for purchase."
            );
        }
    }

    // Find inventory associated with the product
    private Inventory getInventory(int productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found for product ID: "
                                        + productId
                        )
                );
    }

    // Check available stock without reserving it
    private void validateStock(
            Inventory inventory,
            int requestedQuantity) {

        int availableQuantity =
                inventory.getQuantity() - inventory.getReservedQuantity();

        if (requestedQuantity > availableQuantity) {
            throw new ConflictException(
                    "Insufficient stock. Available quantity: "
                            + availableQuantity
            );
        }
    }

    // Build the response and calculate prices on the server
    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findByCartId(cart.getId())
                .stream()
                .map(this::toCartItemResponse)
                .toList();

        int totalItems = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItemResponse item : items) {
            totalItems = Math.addExact(
                    totalItems,
                    item.getQuantity()
            );

            totalPrice = totalPrice.add(item.getSubtotal());
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    // Convert a cart entity to its response DTO
    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();

        BigDecimal subtotal = product.getPrice().multiply(
                BigDecimal.valueOf(cartItem.getQuantity())
        );

        return CartItemResponse.builder()
                .cartItemId(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .unitPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
