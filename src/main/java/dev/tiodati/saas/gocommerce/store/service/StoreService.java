package dev.tiodati.saas.gocommerce.store.service;

import dev.tiodati.saas.gocommerce.store.SchemaManager;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.model.Store;
import dev.tiodati.saas.gocommerce.store.model.StoreAdmin;
import dev.tiodati.saas.gocommerce.store.model.StoreStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import io.quarkus.logging.Log;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class StoreService {

    private final EntityManager em;
    private final SchemaManager schemaManager;
    private final StoreSettingsService settingsService;
    
    @Inject
    public StoreService(EntityManager em, SchemaManager schemaManager, StoreSettingsService settingsService) {
        this.em = em;
        this.schemaManager = schemaManager;
        this.settingsService = settingsService;
    }
    
    /**
     * Get a store by its ID
     */
    public Optional<Store> findById(UUID id) {
        Store store = em.find(Store.class, id);
        return Optional.ofNullable(store);
    }
    
    /**
     * Get the schema name for a store
     * 
     * @param storeId The ID of the store
     * @return The schema name for the store, or null if store not found
     */
    public String getStoreSchemaName(UUID storeId) {
        return findById(storeId)
            .map(Store::getSchemaName)
            .orElseGet(() -> {
                Log.warn("Schema name not found for store ID: " + storeId);
                return null;
            });
    }
    
    /**
     * Get a store by its unique key
     */
    public Optional<Store> findByStoreKey(String storeKey) {
        try {
            Store store = em.createQuery(
                    "SELECT t FROM Store t WHERE t.storeKey = :storeKey", Store.class)
                    .setParameter("storeKey", storeKey)
                    .getSingleResult();
            return Optional.of(store);
        } catch (Exception e) {
            Log.debug("Store not found with key: " + storeKey);
            return Optional.empty();
        }
    }
    
    /**
     * Get a store by its subdomain
     */
    public Optional<Store> findBySubdomain(String subdomain) {
        try {
            Store store = em.createQuery(
                    "SELECT t FROM Store t WHERE t.subdomain = :subdomain", Store.class)
                    .setParameter("subdomain", subdomain)
                    .getSingleResult();
            return Optional.of(store);
        } catch (Exception e) {
            Log.debug("Store not found with subdomain: " + subdomain);
            return Optional.empty();
        }
    }
    
    /**
     * List all stores
     */
    public List<Store> listAll() {
        return em.createQuery("SELECT t FROM Store t WHERE t.isDeleted = false ORDER BY t.name", Store.class)
                .getResultList();
    }
    
    /**
     * List all active stores
     */
    public List<Store> listActive() {
        return em.createQuery(
                "SELECT t FROM Store t WHERE t.status = :status AND t.isDeleted = false ORDER BY t.name", Store.class)
                .setParameter("status", StoreStatus.ACTIVE)
                .getResultList();
    }
    
    /**
     * Create a new store with default schema and admin user
     */
    public Store createStore(Store store, StoreAdmin admin) {
        if (store.getId() != null) {
            throw new IllegalArgumentException("New store cannot have an ID");
        }
        
        // Normalize subdomain to lowercase
        store.setSubdomain(store.getSubdomain().toLowerCase());
        
        // Generate schema name if not provided
        if (store.getSchemaName() == null || store.getSchemaName().isEmpty()) {
            store.setSchemaName("store_" + store.getStoreKey().toLowerCase());
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
                Log.error("Failed to create schema for store: " + store.getName(), e);
                throw new PersistenceException("Failed to create schema", e);
            }
            
            Log.info("Created new store: " + store.getName() + " with key: " + store.getStoreKey());
            return store;
        } catch (PersistenceException e) {
            Log.error("Failed to create store: " + store.getName(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }
    
    /**
     * Update an existing store
     */
    public Store updateStore(Store store) {
        if (store.getId() == null) {
            throw new IllegalArgumentException("Cannot update store without ID");
        }
        
        Store existingStore = em.find(Store.class, store.getId());
        if (existingStore == null) {
            throw new IllegalArgumentException("Store not found with ID: " + store.getId());
        }
        
        // Cannot change store key or schema name after creation
        store.setStoreKey(existingStore.getStoreKey());
        store.setSchemaName(existingStore.getSchemaName());
        
        return em.merge(store);
    }
    
    /**
     * Change store status
     */
    public Store updateStoreStatus(UUID storeId, StoreStatus status) {
        Store store = em.find(Store.class, storeId);
        if (store == null) {
            throw new IllegalArgumentException("Store not found with ID: " + storeId);
        }
        
        store.setStatus(status);
        return store;
    }
    
    /**
     * Soft delete a store
     */
    public void deleteStore(UUID id) {
        Store store = em.find(Store.class, id);
        if (store != null) {
            store.setDeleted(true);
            em.merge(store);
        }
    }
    
    /**
     * Execute a function within a specific store context
     * @param <T> The return type of the function
     * @param schemaName The store schema to use
     * @param function The function to execute
     * @return The result of the function execution
     */
    public <T> T executeInStoreContext(String schemaName, java.util.function.Supplier<T> function) {
        String previousStore = StoreContext.getCurrentStore();
        try {
            // Set the store context for this execution
            StoreContext.setCurrentStore(schemaName);
            // Execute the function in the store context
            return function.get();
        } finally {
            // Restore previous store context
            if (previousStore != null) {
                StoreContext.setCurrentStore(previousStore);
            } else {
                StoreContext.clear();
            }
        }
    }
    
    /**
     * Execute a function within a specific store context (void version)
     * @param schemaName The store schema to use
     * @param function The function to execute
     */
    public void executeInStoreContext(String schemaName, Runnable function) {
        String previousStore = StoreContext.getCurrentStore();
        try {
            Log.debug("Executing in store context: " + schemaName);
            StoreContext.setCurrentStore(schemaName);
            function.run();
        } finally {
            Log.debug("Restoring previous store context: " + previousStore);
            StoreContext.setCurrentStore(previousStore);
        }
    }
}