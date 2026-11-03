package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private Inventory inventory;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("test@example.com");

        cart = new Cart();
        cart.setId(10);
        cart.setUser(user);

        product = new Product();
        product.setId(100);
        product.setName("Laptop");
        product.setSku("LAPTOP-001");
        product.setPrice(new BigDecimal("50000.00"));
        product.setStatus(ProductStatus.ACTIVE);

        inventory = new Inventory();
        inventory.setId(20);
        inventory.setProduct(product);
        inventory.setQuantity(20);
        inventory.setReservedQuantity(5);

        cartItem = new CartItem();
        cartItem.setId(30);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Helper: authenticate the test user
    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null,
                        List.of()
                )
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
    }

    // Helper: prepare an existing cart
    private void prepareExistingCart() {
        authenticateUser();

        when(cartRepository.findByUserId(1))
                .thenReturn(Optional.of(cart));
    }

    // Helper: create AddToCartRequest
    private AddToCartRequest addRequest(int productId, int quantity) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    // Helper: create UpdateCartItemRequest
    private UpdateCartItemRequest updateRequest(int quantity) {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(quantity);
        return request;
    }

    // 1. Get existing cart successfully
    @Test
    void getMyCart_shouldReturnExistingCart() {
        prepareExistingCart();

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of());

        CartResponse response = cartService.getMyCart();

        assertNotNull(response);
        assertEquals(10, response.getCartId());
        assertEquals(1, response.getUserId());
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());

        verify(cartRepository, never()).save(any(Cart.class));
    }

    // 2. Create a cart when the user does not have one
    @Test
    void getMyCart_shouldCreateCartWhenOneDoesNotExist() {
        authenticateUser();

        when(cartRepository.findByUserId(1))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class)))
                .thenAnswer(invocation -> {
                    Cart savedCart = invocation.getArgument(0);
                    savedCart.setId(10);
                    return savedCart;
                });

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of());

        CartResponse response = cartService.getMyCart();

        assertNotNull(response);
        assertEquals(10, response.getCartId());

        verify(cartRepository).save(any(Cart.class));
    }

    // 3. Reject unauthenticated access
    @Test
    void getMyCart_shouldThrowWhenUserIsNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AccessDeniedException.class,
                () -> cartService.getMyCart()
        );

        verifyNoInteractions(userRepository);
    }

    // 4. Reject access when authenticated user cannot be found
    @Test
    void getMyCart_shouldThrowWhenAuthenticatedUserDoesNotExist() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "missing@example.com",
                        null,
                        List.of()
                )
        );

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> cartService.getMyCart()
        );

        verifyNoInteractions(cartRepository);
    }

    // 5. Reject adding a product that does not exist
    @Test
    void addItem_shouldThrowWhenProductDoesNotExist() {
        prepareExistingCart();

        when(productRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addItem(addRequest(999, 1))
        );

        verifyNoInteractions(inventoryRepository);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 6. Reject adding an inactive product
    @Test
    void addItem_shouldThrowWhenProductIsInactive() {
        prepareExistingCart();

        product.setStatus(ProductStatus.INACTIVE);

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        assertThrows(
                ConflictException.class,
                () -> cartService.addItem(addRequest(100, 1))
        );

        verifyNoInteractions(inventoryRepository);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 7. Reject adding a product without inventory
    @Test
    void addItem_shouldThrowWhenInventoryDoesNotExist() {
        prepareExistingCart();

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addItem(addRequest(100, 1))
        );

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 8. Add a new product to the cart
    @Test
    void addItem_shouldAddNewProductSuccessfully() {
        prepareExistingCart();

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.findByCartIdAndProductId(10, 100))
                .thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CartItem savedItem = new CartItem();
        savedItem.setId(30);
        savedItem.setCart(cart);
        savedItem.setProduct(product);
        savedItem.setQuantity(3);

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of(savedItem));

        CartResponse response =
                cartService.addItem(addRequest(100, 3));

        assertNotNull(response);
        assertEquals(3, response.getTotalItems());
        assertEquals(
                new BigDecimal("150000.00"),
                response.getTotalPrice()
        );

        verify(cartItemRepository).save(any(CartItem.class));
    }

    // 9. Increase quantity when the product already exists in the cart
    @Test
    void addItem_shouldIncreaseExistingProductQuantity() {
        prepareExistingCart();

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.findByCartIdAndProductId(10, 100))
                .thenReturn(Optional.of(cartItem));

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of(cartItem));

        CartResponse response =
                cartService.addItem(addRequest(100, 3));

        assertEquals(5, cartItem.getQuantity());
        assertEquals(5, response.getTotalItems());

        verify(cartItemRepository).save(cartItem);
    }

    // 10. Reject adding more than available stock
    @Test
    void addItem_shouldThrowWhenRequestedQuantityExceedsStock() {
        prepareExistingCart();

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.findByCartIdAndProductId(10, 100))
                .thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> cartService.addItem(addRequest(100, 16))
        );

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 11. Reject integer overflow when increasing cart quantity
    @Test
    void addItem_shouldThrowWhenQuantityAdditionOverflows() {
        prepareExistingCart();

        cartItem.setQuantity(Integer.MAX_VALUE);

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.findByCartIdAndProductId(10, 100))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                ConflictException.class,
                () -> cartService.addItem(addRequest(100, 1))
        );

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 12. Update cart item quantity successfully
    @Test
    void updateItem_shouldUpdateQuantitySuccessfully() {
        prepareExistingCart();

        when(cartItemRepository.findById(30))
                .thenReturn(Optional.of(cartItem));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of(cartItem));

        CartResponse response =
                cartService.updateItem(30, updateRequest(5));

        assertEquals(5, cartItem.getQuantity());
        assertEquals(5, response.getTotalItems());

        verify(cartItemRepository).save(cartItem);
    }

    // 13. Reject updating a cart item that does not exist
    @Test
    void updateItem_shouldThrowWhenCartItemDoesNotExist() {
        prepareExistingCart();

        when(cartItemRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateItem(999, updateRequest(2))
        );

        verifyNoInteractions(inventoryRepository);
    }

    // 14. Prevent one user from updating another user's cart item
    @Test
    void updateItem_shouldThrowWhenCartItemBelongsToAnotherCart() {
        prepareExistingCart();

        Cart anotherCart = new Cart();
        anotherCart.setId(99);

        cartItem.setCart(anotherCart);

        when(cartItemRepository.findById(30))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateItem(30, updateRequest(2))
        );

        verifyNoInteractions(inventoryRepository);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 15. Reject updating an inactive product
    @Test
    void updateItem_shouldThrowWhenProductIsInactive() {
        prepareExistingCart();

        product.setStatus(ProductStatus.INACTIVE);

        when(cartItemRepository.findById(30))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                ConflictException.class,
                () -> cartService.updateItem(30, updateRequest(2))
        );

        verifyNoInteractions(inventoryRepository);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 16. Reject updating quantity beyond available stock
    @Test
    void updateItem_shouldThrowWhenQuantityExceedsAvailableStock() {
        prepareExistingCart();

        when(cartItemRepository.findById(30))
                .thenReturn(Optional.of(cartItem));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                ConflictException.class,
                () -> cartService.updateItem(30, updateRequest(16))
        );

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // 17. Remove a cart item successfully
    @Test
    void removeItem_shouldDeleteOwnedCartItem() {
        prepareExistingCart();

        when(cartItemRepository.findById(30))
                .thenReturn(Optional.of(cartItem));

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of());

        CartResponse response = cartService.removeItem(30);

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());

        verify(cartItemRepository).delete(cartItem);
    }

    // 18. Reject removing a cart item that does not exist
    @Test
    void removeItem_shouldThrowWhenCartItemDoesNotExist() {
        prepareExistingCart();

        when(cartItemRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.removeItem(999)
        );

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    // 19. Prevent removing another user's cart item
    @Test
    void removeItem_shouldThrowWhenCartItemBelongsToAnotherCart() {
        prepareExistingCart();

        Cart anotherCart = new Cart();
        anotherCart.setId(99);
        cartItem.setCart(anotherCart);

        when(cartItemRepository.findById(30))
                .thenReturn(Optional.of(cartItem));

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.removeItem(30)
        );

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    // 20. Clear all items from the authenticated user's cart
    @Test
    void clearCart_shouldDeleteAllCartItems() {
        prepareExistingCart();

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of());

        CartResponse response = cartService.clearCart();

        assertNotNull(response);
        assertTrue(response.getItems().isEmpty());
        assertEquals(0, response.getTotalItems());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());

        verify(cartItemRepository).deleteByCartId(10);
    }

    // 21. Calculate subtotal for one cart item
    @Test
    void getMyCart_shouldCalculateItemSubtotal() {
        prepareExistingCart();

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of(cartItem));

        CartResponse response = cartService.getMyCart();

        assertEquals(1, response.getItems().size());
        assertEquals(
                new BigDecimal("100000.00"),
                response.getItems().get(0).getSubtotal()
        );
        assertEquals(
                new BigDecimal("50000.00"),
                response.getItems().get(0).getUnitPrice()
        );
    }

    // 22. Calculate total quantity across multiple cart items
    @Test
    void getMyCart_shouldCalculateTotalQuantity() {
        prepareExistingCart();

        Product secondProduct = new Product();
        secondProduct.setId(101);
        secondProduct.setName("Mouse");
        secondProduct.setSku("MOUSE-001");
        secondProduct.setPrice(new BigDecimal("1000.00"));
        secondProduct.setStatus(ProductStatus.ACTIVE);

        CartItem secondItem = new CartItem();
        secondItem.setId(31);
        secondItem.setCart(cart);
        secondItem.setProduct(secondProduct);
        secondItem.setQuantity(3);

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of(cartItem, secondItem));

        CartResponse response = cartService.getMyCart();

        assertEquals(5, response.getTotalItems());
        assertEquals(2, response.getItems().size());
    }

    // 23. Calculate total price across multiple cart items
    @Test
    void getMyCart_shouldCalculateTotalPrice() {
        prepareExistingCart();

        Product secondProduct = new Product();
        secondProduct.setId(101);
        secondProduct.setName("Mouse");
        secondProduct.setSku("MOUSE-001");
        secondProduct.setPrice(new BigDecimal("1000.00"));
        secondProduct.setStatus(ProductStatus.ACTIVE);

        CartItem secondItem = new CartItem();
        secondItem.setId(31);
        secondItem.setCart(cart);
        secondItem.setProduct(secondProduct);
        secondItem.setQuantity(3);

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of(cartItem, secondItem));

        CartResponse response = cartService.getMyCart();

        assertEquals(
                new BigDecimal("103000.00"),
                response.getTotalPrice()
        );
    }

    // 24. Ensure adding to cart does not reserve inventory
    @Test
    void addItem_shouldNotChangeInventoryReservation() {
        prepareExistingCart();

        when(productRepository.findById(100))
                .thenReturn(Optional.of(product));

        when(inventoryRepository.findByProductId(100))
                .thenReturn(Optional.of(inventory));

        when(cartItemRepository.findByCartIdAndProductId(10, 100))
                .thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(cartItemRepository.findByCartId(10))
                .thenReturn(List.of());

        int originalReservedQuantity =
                inventory.getReservedQuantity();

        cartService.addItem(addRequest(100, 2));

        assertEquals(
                originalReservedQuantity,
                inventory.getReservedQuantity()
        );

        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}
