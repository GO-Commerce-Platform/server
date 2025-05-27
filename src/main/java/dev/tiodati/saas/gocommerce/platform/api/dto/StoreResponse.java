package dev.tiodati.saas.gocommerce.platform.api.dto;

import java.time.OffsetDateTime; // Changed from LocalDateTime
import java.util.UUID;

/**
 * DTO representing the response for store details.
 *
 * @param id         The unique identifier of the store.
 * @param name       The display name of the store.
 * @param subdomain  The unique subdomain of the store.
 * @param fullDomain The full domain name of the store (e.g.,
 *                   subdomain.gocommerce.com).
 * @param status     The current status of the store (e.g., ACTIVE, PENDING).
 * @param createdAt  The timestamp when the store was created.
 */
public record StoreResponse(UUID id, String name, String subdomain,
        String fullDomain, String status, OffsetDateTime createdAt) {
}
