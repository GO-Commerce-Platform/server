package dev.tiodati.saas.gocommerce.cart.service;

import dev.tiodati.saas.gocommerce.cart.dto.CartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.CartSummaryDto;
import dev.tiodati.saas.gocommerce.cart.dto.CreateCartItemDto;
import dev.tiodati.saas.gocommerce.cart.dto.ShoppingCartDto;
import dev.tiodati.saas.gocommerce.cart.dto.UpdateCartItemDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for shopping cart operations.
 * Defines the contract for cart management functionality.
 */
public interface CartService {

    /**
     * Get or create an active cart for a customer.
     * If no active cart exists, creates a new one.
     *
     * @param customerId the customer ID
     * @return shopping cart DTO
     */
    ShoppingCartDto getOrCreateCart(UUID customerId);

    /**
     * Get or create a cart for a guest session.
     * Used for anonymous shopping scenarios.
     *
     * @param sessionId the session ID
     * @return shopping cart DTO
     */
    ShoppingCartDto getOrCreateGuestCart(String sessionId);

    /**
     * Get cart by ID.
     *
     * @param cartId the cart ID
     * @return optional shopping cart DTO
     */
    Optional<ShoppingCartDto> getCartById(UUID cartId);

    /**
     * Get cart summary information.
     * Returns lightweight cart statistics.
     *
     * @param cartId the cart ID
     * @return optional cart summary
     */
    Optional<CartSummaryDto> getCartSummary(UUID cartId);

    /**
     * Add item to cart.
     * If item already exists, increases the quantity.
     *
     * @param cartId        the cart ID
     * @param createItemDto the item to add
     * @return updated cart item DTO
     */
    CartItemDto addItemToCart(UUID cartId, CreateCartItemDto createItemDto);

    /**
     * Update cart item quantity.
     *
     * @param cartId        the cart ID
     * @param itemId        the item ID
     * @param updateItemDto the update information
     * @return updated cart item DTO
     */
    CartItemDto updateCartItem(UUID cartId, UUID itemId, UpdateCartItemDto updateItemDto);

    /**
     * Remove item from cart.
     *
     * @param cartId the cart ID
     * @param itemId the item ID
     */
    void removeItemFromCart(UUID cartId, UUID itemId);

    /**
     * Clear all items from cart.
     *
     * @param cartId the cart ID
     */
    void clearCart(UUID cartId);

    /**
     * Transfer guest cart to customer.
     * Associates an anonymous cart with a customer account.
     *
     * @param sessionId  the guest session ID
     * @param customerId the customer ID
     * @return transferred cart DTO
     */
    Optional<ShoppingCartDto> transferGuestCartToCustomer(String sessionId, UUID customerId);

    /**
     * Deactivate cart.
     * Marks the cart as inactive without deleting it.
     *
     * @param cartId the cart ID
     */
    void deactivateCart(UUID cartId);

    /**
     * Get all cart items for a specific cart.
     *
     * @param cartId the cart ID
     * @return list of cart item DTOs
     */
    List<CartItemDto> getCartItems(UUID cartId);

    /**
     * Calculate cart totals.
     * Recalculates the total amount and item count for the cart.
     *
     * @param cartId the cart ID
     * @return cart summary with updated totals
     */
    CartSummaryDto calculateCartTotals(UUID cartId);

    /**
     * Check if cart exists and is active.
     *
     * @param cartId the cart ID
     * @return true if cart exists and is active
     */
    boolean isCartActive(UUID cartId);

    /**
     * Get cart item count for a customer.
     *
     * @param customerId the customer ID
     * @return total number of items in customer's active cart
     */
    int getCartItemCount(UUID customerId);
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
