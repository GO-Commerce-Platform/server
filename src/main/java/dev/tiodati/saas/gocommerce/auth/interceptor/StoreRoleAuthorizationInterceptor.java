package dev.tiodati.saas.gocommerce.auth.interceptor;

import java.util.Arrays;
import java.util.stream.Collectors;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.auth.service.KeycloakRoleVerificationService;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import io.quarkus.logging.Log;
import io.quarkus.security.ForbiddenException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Interceptor to enforce store-specific role requirements on methods annotated
 * with {@link RequiresStoreRole}. It verifies if the authenticated user
 * possesses any of the specified roles and has access to the current store
 * context.
 */
@RequiresStoreRole({})
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class StoreRoleAuthorizationInterceptor {

    /**
     * Service for verifying Keycloak roles.
     */
    @Inject
    private KeycloakRoleVerificationService roleVerificationService;

    /**
     * Intercepts method invocations to check for required store roles. If the
     * user lacks the necessary roles or access to the current store, a
     * {@link ForbiddenException} is thrown.
     *
     * @param context The invocation context.
     * @return The result of the method invocation if authorization is
     *         successful.
     * @throws Exception if the method invocation fails or access is denied.
     */
    @AroundInvoke
    public Object checkStoreRoles(InvocationContext context) throws Exception {
        RequiresStoreRole annotation = context.getMethod()
                .getAnnotation(RequiresStoreRole.class);

        if (annotation == null) {
            annotation = context.getTarget().getClass()
                    .getAnnotation(RequiresStoreRole.class);
        }

        if (annotation != null) {
            Roles[] requiredRoles = annotation.value();

            if (requiredRoles.length > 0) {
                // Check if user has any of the required roles
                boolean hasRequiredRole = roleVerificationService
                        .hasAnyRole(requiredRoles);

                // If user has a required role, check if they have access to the
                // current store
                if (hasRequiredRole) {
                    String currentStoreId = StoreContext.getCurrentStore();

                    // Platform admin has access to all stores
                    if (roleVerificationService.isPlatformAdmin()) {
                        return context.proceed();
                    }

                    // Check if user is admin for this specific store
                    if (currentStoreId != null && roleVerificationService
                            .isStoreAdmin(currentStoreId)) {
                        return context.proceed();
                    }

                    Log.warnf("Access denied: User lacks access to store %s",
                            currentStoreId);
                    throw new ForbiddenException(
                            "User lacks access to the current store");
                }

                String roleNames = Arrays.stream(requiredRoles)
                        .map(Roles::getRoleName)
                        .collect(Collectors.joining(", "));

                Log.warnf(
                        "Access denied: User lacks required store role(s): %s",
                        roleNames);
                throw new ForbiddenException(
                        "User lacks required store role(s): " + roleNames);
            }
        }

        return context.proceed();
    }
}
