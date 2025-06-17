package dev.tiodati.saas.gocommerce.cart.repository;

import dev.tiodati.saas.gocommerce.cart.entity.CartItem;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CartItem entity operations.
 * Provides methods for cart item management and retrieval.
 */
@ApplicationScoped
public class CartItemRepository implements PanacheRepositoryBase<CartItem, UUID> {

    /**
     * Find all items in a shopping cart.
     *
     * @param cartId the shopping cart ID
     * @return list of cart items
     */
    public List<CartItem> findByCartId(UUID cartId) {
        return find("shoppingCart.id = ?1", cartId).list();
    }

    /**
     * Find specific cart item by cart and product.
     *
     * @param cartId    the shopping cart ID
     * @param productId the product ID
     * @return optional cart item
     */
    public Optional<CartItem> findByCartAndProduct(UUID cartId, UUID productId) {
        return find("shoppingCart.id = ?1 and product.id = ?2", cartId, productId)
                .firstResultOptional();
    }

    /**
     * Find cart items by product ID.
     * Useful for checking which carts contain a specific product.
     *
     * @param productId the product ID
     * @return list of cart items
     */
    public List<CartItem> findByProductId(UUID productId) {
        return find("product.id = ?1", productId).list();
    }

    /**
     * Count items in a shopping cart.
     *
     * @param cartId the shopping cart ID
     * @return number of items in the cart
     */
    public long countItemsInCart(UUID cartId) {
        return count("shoppingCart.id = ?1", cartId);
    }

    /**
     * Calculate total quantity in a shopping cart.
     *
     * @param cartId the shopping cart ID
     * @return total quantity of all items
     */
    public Integer getTotalQuantityInCart(UUID cartId) {
        var result = find("SELECT COALESCE(SUM(quantity), 0) FROM CartItem " +
                "WHERE shoppingCart.id = ?1", cartId)
                .project(Integer.class)
                .firstResult();
        return result != null ? result : 0;
    }

    /**
     * Calculate subtotal for a shopping cart.
     *
     * @param cartId the shopping cart ID
     * @return subtotal amount
     */
    public Double getCartSubtotal(UUID cartId) {
        var result = find("SELECT COALESCE(SUM(quantity * unitPrice), 0.0) FROM CartItem "
                + "WHERE shoppingCart.id = ?1", cartId)
                .project(Double.class)
                .firstResult();
        return result != null ? result : 0.0;
    }

    /**
     * Update cart item quantity.
     *
     * @param cartItemId the cart item ID
     * @param quantity   the new quantity
     * @return number of affected rows
     */
    public int updateQuantity(UUID cartItemId, int quantity) {
        return update("quantity = ?1 where id = ?2", quantity, cartItemId);
    }

    /**
     * Update cart item unit price.
     * Used when product prices change.
     *
     * @param cartItemId the cart item ID
     * @param unitPrice  the new unit price
     * @return number of affected rows
     */
    public int updateUnitPrice(UUID cartItemId, double unitPrice) {
        return update("unitPrice = ?1 where id = ?2", unitPrice, cartItemId);
    }

    /**
     * Remove cart item by cart and product.
     *
     * @param cartId    the shopping cart ID
     * @param productId the product ID
     * @return number of deleted items
     */
    public long removeByCartAndProduct(UUID cartId, UUID productId) {
        return delete("shoppingCart.id = ?1 and product.id = ?2", cartId, productId);
    }

    /**
     * Clear all items from a shopping cart.
     *
     * @param cartId the shopping cart ID
     * @return number of deleted items
     */
    public long clearCart(UUID cartId) {
        return delete("shoppingCart.id = ?1", cartId);
    }

    /**
     * Remove items for discontinued products.
     * Cleans up cart items when products are no longer available.
     *
     * @param productId the discontinued product ID
     * @return number of removed items
     */
    public long removeDiscontinuedProduct(UUID productId) {
        return delete("product.id = ?1", productId);
    }

    /**
     * Find cart items that need price updates.
     * Items where the current product price differs from the cart item price.
     *
     * @return list of cart items with outdated prices
     */
    public List<CartItem> findItemsWithOutdatedPrices() {
        return find("unitPrice != product.price").list();
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
