package dev.tiodati.saas.gocommerce.auth.interceptor;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.quarkus.logging.Log;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresStoreRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.auth.service.KeycloakRoleVerificationService;
import dev.tiodati.saas.gocommerce.store.StoreContext;
import io.quarkus.security.ForbiddenException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@RequiresStoreRole({})
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class StoreRoleAuthorizationInterceptor {

    // private static final Logger LOG = Logger.getLogger(StoreRoleAuthorizationInterceptor.class);

    @Inject
    KeycloakRoleVerificationService roleVerificationService;
    
    @Inject
    StoreContext storeContext;

    @AroundInvoke
    public Object checkStoreRoles(InvocationContext context) throws Exception {
        RequiresStoreRole annotation = context.getMethod().getAnnotation(RequiresStoreRole.class);
        
        if (annotation == null) {
            annotation = context.getTarget().getClass().getAnnotation(RequiresStoreRole.class);
        }
        
        if (annotation != null) {
            Roles[] requiredRoles = annotation.value();
            
            if (requiredRoles.length > 0) {
                // Check if user has any of the required roles
                boolean hasRequiredRole = roleVerificationService.hasAnyRole(requiredRoles);
                
                // If user has a required role, check if they have access to the current store
                if (hasRequiredRole) {
                    String currentStoreId = StoreContext.getCurrentStore();
                    
                    // Platform admin has access to all stores
                    if (roleVerificationService.isPlatformAdmin()) {
                        return context.proceed();
                    }
                    
                    // Check if user is admin for this specific store
                    if (roleVerificationService.isStoreAdmin(currentStoreId)) {
                        return context.proceed();
                    }
                    
                    Log.warnf("Access denied: User lacks access to store %s", currentStoreId);
                    throw new ForbiddenException("User lacks access to the current store");
                }
                
                String roleNames = Arrays.stream(requiredRoles)
                        .map(Roles::getRoleName)
                        .collect(Collectors.joining(", "));
                
                Log.warnf("Access denied: User lacks required store role(s): %s", roleNames);
                throw new ForbiddenException("User lacks required store role(s): " + roleNames);
            }
        }
        
        return context.proceed();
    }
}
