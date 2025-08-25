package dev.tiodati.saas.gocommerce.cart.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.cart.dto.CartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.CartSummaryDto;
import dev.tiodati.saas.gocommerce.cart.dto.CreateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.ShoppingCartDto;
import dev.tiodati.saas.gocommerce.cart.dto.UpdateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.entity.CartItem;
import dev.tiodati.saas.gocommerce.cart.entity.CartStatus;
import dev.tiodati.saas.gocommerce.cart.entity.ShoppingCart;
import dev.tiodati.saas.gocommerce.cart.repository.CartItemRepository;
import dev.tiodati.saas.gocommerce.cart.repository.ShoppingCartRepository;
import dev.tiodati.saas.gocommerce.cart.service.CartService;
import dev.tiodati.saas.gocommerce.customer.entity.Customer;
import dev.tiodati.saas.gocommerce.inventory.service.InventoryService;
import dev.tiodati.saas.gocommerce.product.repository.ProductRepository;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of CartService providing shopping cart functionality.
 * Handles cart operations including item management and cart lifecycle.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /**
     * Repository for shopping cart operations.
     **/
    private final ShoppingCartRepository cartRepository;

    /** Repository for cart item operations. */
    private final CartItemRepository cartItemRepository;

    /** Repository for product operations. */
    private final ProductRepository productRepository;
    
    /** Service for inventory operations. */
    private final InventoryService inventoryService;
    
    /** Store context for multi-tenant operations. */
    private final StoreContext storeContext;
    
    /** Service for cart expiration management. */
    private final dev.tiodati.saas.gocommerce.cart.service.CartExpirationService cartExpirationService;

    @Override
    @Transactional
    public ShoppingCartDto getOrCreateCart(UUID customerId) {
        Log.infof("Getting or creating cart for customer %s", customerId);

        var cartOptional = cartRepository.findActiveByCustomerId(customerId);

        if (cartOptional.isPresent()) {
            return mapToDto(cartOptional.get());
        }

        // Create new cart for customer
        var customer = new Customer();
        customer.setId(customerId);
        customer.setVersion(0); // Initialize version for detached entity

        var cart = ShoppingCart.builder()
                .customer(customer)
                .status(CartStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(30)) // Cart expires in 30 days
                .build();

        cartRepository.persist(cart);
        return mapToDto(cart);
    }

    @Override
    @Transactional
    public ShoppingCartDto getOrCreateGuestCart(String sessionId) {
        Log.infof("Getting or creating guest cart for session %s", sessionId);

        var cartOptional = cartRepository.findBySessionId(sessionId);

        if (cartOptional.isPresent()) {
            return mapToDto(cartOptional.get());
        }

        // Create new guest cart
        var cart = ShoppingCart.builder()
                .sessionId(sessionId)
                .status(CartStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusDays(7)) // Guest cart expires in 7 days
                .build();

        cartRepository.persist(cart);
        return mapToDto(cart);
    }

    @Override
    public Optional<ShoppingCartDto> getCartById(UUID cartId) {
        Log.infof("Getting cart by ID %s", cartId);

        return cartRepository.findByIdOptional(cartId)
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
                .map(this::mapToDto);
    }

    @Override
    public Optional<CartSummaryDto> getCartSummary(UUID cartId) {
        Log.infof("Getting cart summary for cart %s", cartId);

        return cartRepository.findByIdOptional(cartId)
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
                .map(this::mapToSummaryDto);
    }

    @Override
    @Transactional
    public CartItemDto addItemToCart(UUID cartId, CreateCartItemDto createItemDto) {
        Log.infof("Adding item to cart %s: product %s, quantity %d",
                cartId, createItemDto.productId(), createItemDto.quantity());

        // Validate store context for multi-tenant security
        validateStoreContext();
        
        var cart = cartRepository.findByIdOptional(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalArgumentException("Cart is not active: " + cartId);
        }
        
        // Check if cart is expired
        if (cartExpirationService.isCartExpired(cartId)) {
            cartExpirationService.expireCart(cartId);
            throw new IllegalArgumentException("Cart has expired: " + cartId);
        }

        var product = productRepository.findByIdOptional(createItemDto.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + createItemDto.productId()));

        // Check if item already exists in cart
        var existingItem = cartItemRepository.findByCartAndProduct(cartId, createItemDto.productId());

        if (existingItem.isPresent()) {
            // Update existing item quantity
            var item = existingItem.get();
            var newQuantity = item.getQuantity() + createItemDto.quantity();
            
            // Validate stock availability for the new total quantity
            validateStockAvailability(product.getId(), newQuantity);
            
            item.setQuantity(newQuantity);
            cartItemRepository.persist(item);
            updateCartTotals(cart);
            return mapToItemDto(item);
        } else {
            // Validate stock availability for new item
            validateStockAvailability(product.getId(), createItemDto.quantity());
            
            // Create new cart item
            var cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(createItemDto.quantity())
                    .unitPrice(product.getPrice())
                    .build();

            cartItemRepository.persist(cartItem);
            cart.addItem(cartItem);
            updateCartTotals(cart);
            return mapToItemDto(cartItem);
        }
    }

    @Override
    @Transactional
    public CartItemDto updateCartItem(UUID cartId, UUID itemId, UpdateCartItemDto updateItemDto) {
        Log.infof("Updating cart item %s in cart %s with quantity %d",
                itemId, cartId, updateItemDto.quantity());

        // Validate store context for multi-tenant security
        validateStoreContext();
        
        // Check if cart is expired
        if (cartExpirationService.isCartExpired(cartId)) {
            cartExpirationService.expireCart(cartId);
            throw new IllegalArgumentException("Cart has expired: " + cartId);
        }
        
        var cartItem = cartItemRepository.findByIdOptional(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + itemId));

        if (!cartItem.getCart().getId().equals(cartId)) {
            throw new IllegalArgumentException("Cart item does not belong to cart: " + cartId);
        }

        // Validate stock availability for the updated quantity
        validateStockAvailability(cartItem.getProduct().getId(), updateItemDto.quantity());
        
        cartItem.setQuantity(updateItemDto.quantity());
        cartItemRepository.persist(cartItem);

        updateCartTotals(cartItem.getCart());
        return mapToItemDto(cartItem);
    }

    @Override
    @Transactional
    public void removeItemFromCart(UUID cartId, UUID itemId) {
        Log.infof("Removing item %s from cart %s", itemId, cartId);

        var cartItem = cartItemRepository.findByIdOptional(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + itemId));

        if (!cartItem.getCart().getId().equals(cartId)) {
            throw new IllegalArgumentException("Cart item does not belong to cart: " + cartId);
        }

        var cart = cartItem.getCart();
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        updateCartTotals(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID cartId) {
        Log.infof("Clearing cart %s", cartId);

        var cart = cartRepository.findByIdOptional(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        cartItemRepository.clearCart(cartId);
        cart.getItems().clear();
        updateCartTotals(cart);
    }

    @Override
    @Transactional
    public Optional<ShoppingCartDto> transferGuestCartToCustomer(String sessionId, UUID customerId) {
        Log.infof("Transferring guest cart from session %s to customer %s", sessionId, customerId);

        var existingCustomerCart = cartRepository.findActiveByCustomerId(customerId);
        var guestCart = cartRepository.findBySessionId(sessionId);

        if (guestCart.isEmpty()) {
            Log.infof("No guest cart found for session %s", sessionId);
            return Optional.empty();
        }

        var cart = guestCart.get();

        if (existingCustomerCart.isPresent()) {
            // Merge guest cart into existing customer cart
            var customerCart = existingCustomerCart.get();
            mergeCartItems(cart, customerCart);
            cartRepository.deactivateCart(cart.getId());
            return Optional.of(mapToDto(customerCart));
        } else {
            // Transfer guest cart to customer
            var customer = new Customer();
            customer.setId(customerId);
            customer.setVersion(0); // Initialize version for detached entity

            cartRepository.transferGuestCartToCustomer(sessionId, customer);
            cart.setSessionId(null);
            cart.setCustomer(customer);

            return Optional.of(mapToDto(cart));
        }
    }

    @Override
    @Transactional
    public void deactivateCart(UUID cartId) {
        Log.infof("Deactivating cart %s", cartId);
        cartRepository.deactivateCart(cartId);
    }

    @Override
    public List<CartItemDto> getCartItems(UUID cartId) {
        Log.infof("Getting cart items for cart %s", cartId);

        return cartItemRepository.findByCartId(cartId)
                .stream()
                .map(this::mapToItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CartSummaryDto calculateCartTotals(UUID cartId) {
        Log.infof("Calculating totals for cart %s", cartId);

        var cart = cartRepository.findByIdOptional(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        var items = cartItemRepository.findByCartId(cartId);
        var totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        var totalAmount = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartSummaryDto(
                totalItems,
                totalAmount,
                totalItems == 0,
                cart.getExpiresAt() != null && LocalDateTime.now().isAfter(cart.getExpiresAt()));
    }

    @Override
    public boolean isCartActive(UUID cartId) {
        // Query directly from database to avoid stale entities from bulk updates
        return cartRepository.find("id = ?1 and status = ?2", cartId, CartStatus.ACTIVE)
                .firstResultOptional()
                .isPresent();
    }

    @Override
    public int getCartItemCount(UUID customerId) {
        return cartRepository.findActiveByCustomerId(customerId)
                .map(cart -> cartItemRepository.findByCartId(cart.getId())
                        .stream()
                        .mapToInt(CartItem::getQuantity)
                        .sum())
                .orElse(0);
    }

    private void updateCartTotals(ShoppingCart cart) {
        var items = cartItemRepository.findByCartId(cart.getId());
        var totalAmount = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cartRepository.updateTotalAmount(cart.getId(), totalAmount.doubleValue());
    }

    private void mergeCartItems(ShoppingCart fromCart, ShoppingCart toCart) {
        var fromItems = cartItemRepository.findByCartId(fromCart.getId());

        for (var fromItem : fromItems) {
            var existingItem = cartItemRepository.findByCartAndProduct(
                    toCart.getId(), fromItem.getProduct().getId());

            if (existingItem.isPresent()) {
                // Merge quantities
                var existing = existingItem.get();
                existing.setQuantity(existing.getQuantity() + fromItem.getQuantity());
                cartItemRepository.persist(existing);
            } else {
                // Move item to customer cart
                fromItem.setCart(toCart);
                cartItemRepository.persist(fromItem);
            }
        }

        updateCartTotals(toCart);
    }

    private ShoppingCartDto mapToDto(ShoppingCart cart) {
        var items = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(this::mapToItemDto)
                .toList();

        var totalItems = items.stream().mapToInt(CartItemDto::quantity).sum();
        var totalAmount = items.stream()
                .map(CartItemDto::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ShoppingCartDto(
                cart.getId(),
                cart.getCustomer() != null ? cart.getCustomer().getId() : null,
                cart.getSessionId(),
                cart.getStatus() == CartStatus.ACTIVE,
                cart.getExpiresAt(),
                cart.getCreatedAt(),
                cart.getUpdatedAt(),
                items,
                totalItems,
                totalAmount);
    }

    private CartSummaryDto mapToSummaryDto(ShoppingCart cart) {
        var items = cartItemRepository.findByCartId(cart.getId());
        var totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        var totalAmount = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartSummaryDto(
                totalItems,
                totalAmount,
                totalItems == 0,
                cart.getExpiresAt() != null && LocalDateTime.now().isAfter(cart.getExpiresAt()));
    }

    private CartItemDto mapToItemDto(CartItem item) {
        return new CartItemDto(
                item.getId(),
                item.getCart().getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getAddedAt(),
                item.getUpdatedAt());
    }
    
    /**
     * Validates the current store context for multi-tenant security.
     * Ensures operations are performed in the correct tenant context.
     */
    private void validateStoreContext() {
        var currentStoreId = storeContext.getCurrentStoreId();
        if (currentStoreId == null) {
            throw new IllegalStateException("Store context is not set. Multi-tenant validation failed.");
        }
        Log.debugf("Validated store context: %s", currentStoreId);
    }
    
    /**
     * Validates stock availability for a product and quantity.
     * Uses the inventory service to check if sufficient stock is available.
     *
     * @param productId the product ID to check
     * @param quantity the required quantity
     * @throws IllegalArgumentException if insufficient stock
     */
    private void validateStockAvailability(UUID productId, Integer quantity) {
        var storeIdString = storeContext.getCurrentStoreId();
        if (storeIdString == null) {
            throw new IllegalStateException("Store context is not set for stock validation");
        }
        
        var storeId = UUID.fromString(storeIdString);
        if (!inventoryService.hasSufficientStock(storeId, productId, quantity)) {
            throw new IllegalArgumentException(
                String.format("Insufficient stock for product %s. Requested: %d", productId, quantity));
        }
        Log.debugf("Stock validation passed for product %s, quantity %d", productId, quantity);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
