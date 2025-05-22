package dev.tiodati.saas.gocommerce.platform.service.impl;

import org.jboss.logging.Logger;

import dev.tiodati.saas.gocommerce.platform.api.dto.CreateStoreRequest;
import dev.tiodati.saas.gocommerce.platform.api.dto.StoreResponse;
import dev.tiodati.saas.gocommerce.platform.entity.PlatformStore;
import dev.tiodati.saas.gocommerce.platform.entity.StoreStatus;
import dev.tiodati.saas.gocommerce.platform.repository.PlatformStoreRepository;
import dev.tiodati.saas.gocommerce.platform.service.PlatformAdminService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;

@ApplicationScoped
public class PlatformAdminServiceImpl implements PlatformAdminService {
    
    private static final Logger LOG = Logger.getLogger(PlatformAdminServiceImpl.class);
    
    private final PlatformStoreRepository storeRepository;
    // For now, we'll mock Keycloak admin and schema creation
    // private final KeycloakAdminClient keycloakClient;
    // private final DatabaseSchemaService schemaService;
    
    @Inject
    public PlatformAdminServiceImpl(PlatformStoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }
    
    @Override
    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        LOG.info("Creating new store: " + request.name() + " with subdomain: " + request.subdomain());
        
        // Check if subdomain is available
        if (storeRepository.existsBySubdomain(request.subdomain())) {
            LOG.warn("Subdomain " + request.subdomain() + " already exists");
            throw new WebApplicationException("Store with this subdomain already exists", Response.Status.CONFLICT);
        }
        
        try {
            // Create store entity with explicit timestamps
            OffsetDateTime now = OffsetDateTime.now();
            PlatformStore store = PlatformStore.builder()
                .name(request.name())
                .subdomain(request.subdomain())
                .domainSuffix("gocommerce.com")
                .status(StoreStatus.PENDING_SETUP)
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
            
            // TODO: Create Keycloak realm for the store
            // String realmId = keycloakClient.createRealm(request.subdomain());
            // store.setKeycloakRealmId(realmId);
            
            // TODO: Create store admin user in Keycloak
            // keycloakClient.createUser(realmId, request.adminUser());
            
            // TODO: Create database schema for the store
            // String schemaName = "store_" + request.subdomain().replaceAll("-", "_");
            // schemaService.createSchema(schemaName);
            // store.setDatabaseSchema(schemaName);
            
            // Save the store
            storeRepository.persist(store);
            
            // TODO: In a real implementation, we would have async tasks for complete setup
            // For now, we'll just mark it as active
            store.setStatus(StoreStatus.ACTIVE);
            storeRepository.persist(store);
            
            LOG.info("Store created successfully with ID: " + store.getId());
            
            // Return the response DTO with explicitly set timestamp
            return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getSubdomain(),
                store.getFullDomain(),
                store.getStatus().toString(),
                now  // Always use the explicit timestamp we created
            );
        } catch (PersistenceException e) {
            LOG.error("Error creating store: " + e.getMessage(), e);
            throw new WebApplicationException("Failed to create store: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
