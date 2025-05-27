package dev.tiodati.saas.gocommerce.exception.custom;

/**
 * Exception thrown when an attempt is made to create a resource that already
 * exists (e.g., a store with a subdomain that is already in use).
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException with the specified detail
     * message.
     * 
     * @param message the detail message.
     */
    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateResourceException with the specified detail
     * message and cause.
     * 
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
