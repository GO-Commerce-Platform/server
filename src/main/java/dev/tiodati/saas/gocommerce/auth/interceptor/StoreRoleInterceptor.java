package dev.tiodati.saas.gocommerce.auth.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.logging.Log;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.auth.service.PermissionValidator;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.service.StoreService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;

@Interceptor
@RequiresStoreRole({})
/**
 * Interceptor for handling store-specific role-based access control.
 * This interceptor checks if the user has the required roles for the store
 * contex
 * and sets the store context accordingly.
 *
 * It is applied to methods or classes annotated with @RequiresStoreRole.
 */
@Priority(Interceptor.Priority.APPLICATION)
public class StoreRoleInterceptor {

    @Inject
    JsonWebToken jwt;

    @Inject
    StoreService storeService;

    private final PermissionValidator permissionValidator;

    @Context
    UriInfo uriInfo;

    @Inject
    public StoreRoleInterceptor(PermissionValidator permissionValidator) {
        this.permissionValidator = permissionValidator;
    }

    @AroundInvoke
    public Object verifyStoreRole(InvocationContext context) throws Exception {
        RequiresStoreRole annotation = getAnnotation(context);
        if (annotation == null) {
            return context.proceed();
        }

        // Check if user has admin role (admins bypass store checks)
        Log.infof("Verifying store role for user: %s", jwt.getSubject());
        boolean isAdmin = permissionValidator.hasRole(Roles.PLATFORM_ADMIN);
        Log.infof("User roles: %s", jwt.getGroups());
        Log.infof("Is admin: %s", isAdmin);

        try {
            UUID storeId = extractStoreId();
            Log.infof("Extracted store ID: %s", storeId);

            if (storeId != null) {
                String previousStore = StoreContext.getCurrentStore();
                try {
                    StoreContext.setCurrentStore(storeService.getStoreSchemaName(storeId));
                    Log.infof("Set store context to: %s", StoreContext.getCurrentStore());

                    if (!isAdmin) {
                        boolean hasStoreAccess = permissionValidator.hasStoreAccess(storeId);
                        Log.infof("Has store access: %s", hasStoreAccess);
                        if (!hasStoreAccess) {
                            Log.warn("User attempted to access unauthorized store " + storeId);
                            throw new ForbiddenException("Access denied to store resources");
                        }

                        if (annotation.value().length > 0) {
                            boolean hasRequiredRole = false;
                            for (Roles requiredRole : annotation.value()) {
                                if (permissionValidator.hasStoreRole(storeId, requiredRole)) {
                                    hasRequiredRole = true;
                                    break;
                                }
                            }
                            Log.infof("Has required role: %s", hasRequiredRole);

                            if (!hasRequiredRole) {
                                Log.warn("User lacks required store role. Required: " +
                                        Arrays.toString(annotation.value()));
                                throw new ForbiddenException("Insufficient store permissions");
                            }
                        }
                    }

                    return context.proceed();
                } finally {
                    if (previousStore != null) {
                        StoreContext.setCurrentStore(previousStore);
                    } else {
                        StoreContext.clear();
                    }
                    Log.infof("Restored store context to: %s", StoreContext.getCurrentStore());
                }
            } else if (annotation.requireStoreAccess()) {
                throw new ForbiddenException("Store ID is required for this operation");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            Log.error("Error in store role verification", e);
            throw new ForbiddenException("Error verifying store access permissions");
        }

        return context.proceed();
    }

    private RequiresStoreRole getAnnotation(InvocationContext context) {
        RequiresStoreRole annotation = context.getMethod().getAnnotation(RequiresStoreRole.class);
        if (annotation == null) {
            annotation = context.getTarget().getClass().getAnnotation(RequiresStoreRole.class);
        }
        return annotation;
    }

    private UUID extractStoreId() {
        if (uriInfo == null) {
            Log.warn("UriInfo is null, cannot extract store ID from path");
            return null;
        }

        List<PathSegment> segments = uriInfo.getPathSegments();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getPath().equals("stores") && i + 1 < segments.size()) {
                try {
                    return UUID.fromString(segments.get(i + 1).getPath());
                } catch (IllegalArgumentException e) {
                    Log.debug("Invalid store ID in URL");
                    return null;
                }
            }
        }
        return null;
    }
}
