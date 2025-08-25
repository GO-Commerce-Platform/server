package dev.tiodati.saas.gocommerce.cart.service;

import java.time.LocalDateTime;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.cart.entity.CartStatus;
import dev.tiodati.saas.gocommerce.cart.repository.ShoppingCartRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for managing cart expiration and cleanup.
 * Handles automated cleanup of expired carts and provides expiration validation.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class CartExpirationService {

    private final ShoppingCartRepository cartRepository;

    /**
     * Cleanup expired carts manually.
     * This method can be called periodically or on-demand to clean up expired carts.
     * Marks expired carts as EXPIRED status.
     * 
     * @return number of carts marked as expired
     */
    @Transactional
    public int cleanupExpiredCarts() {
        Log.info("Starting cleanup of expired carts");
        
        try {
            var now = LocalDateTime.now();
            var expiredCount = cartRepository.markExpiredCartsAsExpired(now);
            
            if (expiredCount > 0) {
                Log.infof("Marked %d expired carts as EXPIRED", expiredCount);
            } else {
                Log.debug("No expired carts found during cleanup");
            }
            
            return expiredCount;
            
        } catch (Exception e) {
            Log.errorf(e, "Error during cart expiration cleanup: %s", e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if a specific cart is expired.
     * 
     * @param cartId the cart ID to check
     * @return true if the cart is expired, false otherwise
     */
    public boolean isCartExpired(UUID cartId) {
        return cartRepository.findByIdOptional(cartId)
                .map(cart -> {
                    if (cart.getExpiresAt() == null) {
                        return false;
                    }
                    return LocalDateTime.now().isAfter(cart.getExpiresAt());
                })
                .orElse(false);
    }

    /**
     * Manually expires a cart immediately.
     * 
     * @param cartId the cart ID to expire
     * @return true if the cart was successfully expired, false if not found
     */
    @Transactional
    public boolean expireCart(UUID cartId) {
        Log.infof("Manually expiring cart: %s", cartId);
        
        return cartRepository.findByIdOptional(cartId)
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
                .map(cart -> {
                    cart.setStatus(CartStatus.EXPIRED);
                    cart.setUpdatedAt(LocalDateTime.now());
                    cartRepository.persist(cart);
                    Log.infof("Cart %s marked as expired", cartId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Extends the expiration time for a cart.
     * Useful for keeping active carts alive during user sessions.
     * 
     * @param cartId the cart ID to extend
     * @param additionalHours additional hours to extend the cart
     * @return true if successfully extended, false if cart not found or not active
     */
    @Transactional
    public boolean extendCartExpiration(UUID cartId, int additionalHours) {
        Log.infof("Extending cart %s expiration by %d hours", cartId, additionalHours);
        
        return cartRepository.findByIdOptional(cartId)
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
                .map(cart -> {
                    var newExpiration = cart.getExpiresAt() != null 
                        ? cart.getExpiresAt().plusHours(additionalHours)
                        : LocalDateTime.now().plusHours(additionalHours);
                    
                    cart.setExpiresAt(newExpiration);
                    cart.setUpdatedAt(LocalDateTime.now());
                    cartRepository.persist(cart);
                    
                    Log.infof("Cart %s expiration extended to %s", cartId, newExpiration);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Gets the count of expired carts that need cleanup.
     * Useful for monitoring and metrics.
     * 
     * @return number of carts that are expired but not yet marked as EXPIRED
     */
    public long getExpiredCartCount() {
        var now = LocalDateTime.now();
        return cartRepository.countExpiredCarts(now);
    }

    /**
     * Permanently deletes carts that have been in EXPIRED status for a certain period.
     * This is for data retention management.
     * 
     * @param daysOld number of days a cart should be expired before permanent deletion
     * @return number of carts permanently deleted
     */
    @Transactional
    public int deleteOldExpiredCarts(int daysOld) {
        Log.infof("Deleting expired carts older than %d days", daysOld);
        
        var cutoffDate = LocalDateTime.now().minusDays(daysOld);
        var deletedCount = cartRepository.deleteOldExpiredCarts(cutoffDate);
        
        if (deletedCount > 0) {
            Log.infof("Permanently deleted %d old expired carts", deletedCount);
        }
        
        return deletedCount;
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
