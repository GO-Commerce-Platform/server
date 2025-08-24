package dev.tiodati.saas.gocommerce.store.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.tiodati.saas.gocommerce.platform.SchemaManager;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.entity.Store;
import dev.tiodati.saas.gocommerce.store.entity.StoreAdmin;
import dev.tiodati.saas.gocommerce.store.entity.StoreStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class StoreService {

    /**
     * Service for managing stores in the application.
     * Provides methods to create, update, delete, and retrieve stores,
     * as well as execute operations within a specific store context.
     */
    private final EntityManager em;

    /**
     * Manages database schemas for stores.
     * Provides methods to create and manage schemas for each store.
     */
    private final SchemaManager schemaManager;

    /**
     * Service for managing store settings.
     * Provides methods to apply default settings and manage store configurations.
     */
    private final StoreSettingsService settingsService;

    @Inject
    public StoreService(EntityManager em, SchemaManager schemaManager,
            StoreSettingsService settingsService) {
        this.em = em;
        this.schemaManager = schemaManager;
        this.settingsService = settingsService;
    }

    /**
     * Get a store by its ID.
     *
     * @return An Optional containing the store if found, or empty if not.
     * @param id The ID of the store to retrieve.
     * @throws IllegalArgumentException if the ID is null.
     * @throws PersistenceException     if there is an error retrieving the store.
     **/
    public Optional<Store> findById(UUID id) {
        Store store = em.find(Store.class, id);
        return Optional.ofNullable(store);
    }

    /**
     * Get the schema name for a store.
     *
     * @param storeId The ID of the store
     * @return The schema name for the store, or null if store not found
     */
    public String getStoreSchemaName(UUID storeId) {
        return findById(storeId).map(Store::getSchemaName).orElseGet(() -> {
            Log.warn("Schema name not found for store ID: " + storeId);
            return null;
        });
    }

    /**
     * Get a store by its unique key.
     *
     * @param storeKey The unique key of the store
     * @return An Optional containing the store if found, or empty if not.
     * @throws IllegalArgumentException if the storeKey is null or empty.
     * @throws PersistenceException     if there is an error retrieving the store.
     * @throws IllegalStateException    if the store is not found.
     */
    public Optional<Store> findByStoreKey(String storeKey) {
        try {
            Store store = em.createQuery(
                    "SELECT t FROM Store t WHERE t.storeKey = :storeKey",
                    Store.class).setParameter("storeKey", storeKey)
                    .getSingleResult();
            return Optional.of(store);
        } catch (Exception e) {
            Log.debug("Store not found with key: " + storeKey);
            return Optional.empty();
        }
    }

    /**
     * Get a store by its subdomain.
     *
     * @param subdomain The subdomain of the store
     * @return An Optional containing the store if found, or empty if not.
     * @throws IllegalArgumentException if the subdomain is null or empty.
     * @throws PersistenceException     if there is an error retrieving the store.
     * @throws IllegalStateException    if the store is not found.
     */
    public Optional<Store> findBySubdomain(String subdomain) {
        try {
            Store store = em.createQuery(
                    "SELECT t FROM Store t WHERE t.subdomain = :subdomain",
                    Store.class).setParameter("subdomain", subdomain)
                    .getSingleResult();
            return Optional.of(store);
        } catch (Exception e) {
            Log.debug("Store not found with subdomain: " + subdomain);
            return Optional.empty();
        }
    }

    /**
     * List all stores.
     *
     * @return A list of all stores that are not deleted, ordered by name.
     * @throws PersistenceException     if there is an error retrieving the stores.
     * @throws IllegalStateException    if no stores are found.
     * @throws IllegalArgumentException if the query fails.
     * @throws NullPointerException     if the EntityManager is not initialized.
     * @throws ClassCastException       if the result cannot be cast to List<Store>.
     */
    public List<Store> listAll() {
        return em.createQuery(
                "SELECT t FROM Store t WHERE t.status NOT IN (:excludedStatus1, :excludedStatus2) ORDER BY t.name",
                Store.class)
                .setParameter("excludedStatus1", StoreStatus.DELETED)
                .setParameter("excludedStatus2", StoreStatus.ARCHIVED)
                .getResultList();
    }

    /**
     * List all active stores.
     *
     * @return A list of active stores, ordered by name.
     * @throws PersistenceException     if there is an error retrieving the stores.
     * @throws IllegalStateException    if no stores are found.
     * @throws IllegalArgumentException if the query fails.
     * @throws NullPointerException     if the EntityManager is not initialized.
     * @throws ClassCastException       if the result cannot be cast to List<Store>.
     */
    public List<Store> listActive() {
        return em.createQuery(
                "SELECT t FROM Store t WHERE t.status = :status ORDER BY t.name",
                Store.class).setParameter("status", StoreStatus.ACTIVE)
                .getResultList();
    }

    /**
     * Create a new store with default schema and admin user.
     *
     * @param store The store to create
     * @param admin The admin user for the store
     * @return The created store with generated ID and schema name
     * @throws IllegalArgumentException if the store already has an ID or
     *                                  if the store key or subdomain is invalid.
     * @throws PersistenceException     if there is an error creating the store.
     * @throws SQLException             if there is an error creating the schema.
     * @throws IllegalStateException    if the store cannot be persisted.
     * @throws NullPointerException     if the EntityManager or SchemaManager is not
     *                                  initialized.
     * @throws ClassCastException       if the store cannot be cast to Store.
     */
    public Store createStore(Store store, StoreAdmin admin) {
        if (store.getId() != null) {
            throw new IllegalArgumentException("New store cannot have an ID");
        }

        // Normalize subdomain to lowercase
        store.setSubdomain(store.getSubdomain().toLowerCase());

        // Generate schema name if not provided
        if (store.getSchemaName() == null || store.getSchemaName().isEmpty()) {
            store.setSchemaName("store_" + store.getStoreKey().toLowerCase().replace("-", "_"));
        }

        try {
            // Apply default settings
            store = settingsService.applyDefaultSettings(store);

            // Persist the store
            em.persist(store);
            em.flush(); // Ensure ID is generated

            // Associate admin with store
            admin.setStore(store);
            em.persist(admin);

            // Create schema for store
            try {
                schemaManager.createSchema(store.getSchemaName());
            } catch (SQLException e) {
                // Rollback transaction if schema creation fails
                Log.error(
                        "Failed to create schema for store: " + store.getName(),
                        e);
                throw new PersistenceException("Failed to create schema", e);
            }

            Log.info("Created new store: " + store.getName() + " with key: "
                    + store.getStoreKey());
            return store;
        } catch (PersistenceException e) {
            Log.error("Failed to create store: " + store.getName(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    /**
     * Update an existing store.
     *
     * @param store The store to update
     * @return The updated store
     * @throws IllegalArgumentException      if the store ID is null or if the store
     *                                       does not exist.
     * @throws PersistenceException          if there is an error updating the
     *                                       store.
     * @throws IllegalStateException         if the store cannot be persisted.
     * @throws NullPointerException          if the EntityManager is not
     *                                       initialized.
     * @throws ClassCastException            if the store cannot be cast to Store.
     * @throws UnsupportedOperationException if the store key or schema name is
     *                                       attempted to be changed after creation.
     */
    public Store updateStore(Store store) {
        if (store.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot update store without ID");
        }

        Store existingStore = em.find(Store.class, store.getId());
        if (existingStore == null) {
            throw new IllegalArgumentException(
                    "Store not found with ID: " + store.getId());
        }

        // Cannot change store key or schema name after creation
        store.setStoreKey(existingStore.getStoreKey());
        store.setSchemaName(existingStore.getSchemaName());

        return em.merge(store);
    }

    /**
     * Change store status.
     *
     * @param storeId The ID of the store to update
     * @param status  The new status to set for the store
     * @return The updated store with new status
     * @throws IllegalArgumentException      if the store ID is null or if the store
     *                                       does not exist.
     * @throws PersistenceException          if there is an error updating the store
     *                                       status.
     * @throws IllegalStateException         if the store cannot be persisted.
     * @throws NullPointerException          if the EntityManager is not
     *                                       initialized.
     * @throws ClassCastException            if the store cannot be cast to Store.
     * @throws UnsupportedOperationException if the store status is not valid.
     */
    public Store updateStoreStatus(UUID storeId, StoreStatus status) {
        Store store = em.find(Store.class, storeId);
        if (store == null) {
            throw new IllegalArgumentException(
                    "Store not found with ID: " + storeId);
        }

        store.setStatus(status);
        return store;
    }

    /**
     * Soft delete a store.
     * Marks the store as deleted without removing it from the database.
     *
     * @param id The ID of the store to delete
     * @throws IllegalArgumentException      if the ID is null or if the store does
     *                                       not exist.
     * @throws PersistenceException          if there is an error deleting the
     *                                       store.
     * @throws IllegalStateException         if the store cannot be persisted.
     * @throws NullPointerException          if the EntityManager is not
     *                                       initialized.
     * @throws ClassCastException            if the store cannot be cast to Store.
     * @throws UnsupportedOperationException if the store is already deleted.
     */
    public void deleteStore(UUID id) {
        Store store = em.find(Store.class, id);
        if (store != null) {
            if (store.getStatus() == StoreStatus.DELETED || store.getStatus() == StoreStatus.DELETING) {
                Log.warnf("Store %s is already in status %s. No action taken.", id, store.getStatus());
                return;
            }
            store.setStatus(StoreStatus.DELETING); // Or DELETED, depending on desired final state for this action
            store.setUpdatedAt(java.time.Instant.now());
            em.merge(store);
            Log.infof("Store %s marked as DELETING.", id);
        } else {
            Log.warnf("Store with ID %s not found for deletion.", id);
        }
    }

    /**
     * Execute a function within a specific store contex.
     *
     * @param <T>        The return type of the function
     * @param schemaName The store schema to use
     * @param function   The function to execute
     * @return The result of the function execution
     */
    public <T> T executeInStoreContext(String schemaName,
            java.util.function.Supplier<T> function) {
        String previousStore = StoreContext.getCurrentStore();
        try {
            // Set the store context for this execution
            StoreContext.setCurrentStore(schemaName);
            // Execute the function in the store contex
            return function.get();
        } finally {
            // Restore previous store contex
            if (previousStore != null) {
                StoreContext.setCurrentStore(previousStore);
            } else {
                StoreContext.clear();
            }
        }
    }
}
