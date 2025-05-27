package dev.tiodati.saas.gocommerce.exception.custom;

/**
 * Exception thrown when a requested resource cannot be found. This is often
 * used when an entity lookup by ID or other unique identifier yields no result.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified detail
     * message.
     * 
     * @param message the detail message.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail
     * message and cause.
     * 
     * @param message the detail message.
     * @param cause   the cause of the exception.
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
