package dev.tiodati.saas.gocommerce.auth.interceptor;

import java.util.Arrays;
import java.util.stream.Collectors;

import dev.tiodati.saas.gocommerce.auth.annotation.RequiresRole;
import dev.tiodati.saas.gocommerce.auth.model.Roles;
import dev.tiodati.saas.gocommerce.auth.service.KeycloakRoleVerificationService;
import io.quarkus.logging.Log;
import io.quarkus.security.ForbiddenException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Interceptor to enforce general role requirements on methods annotated with
 * {@link RequiresRole}. It verifies if the authenticated user possesses any of
 * the specified roles.
 */
@RequiresRole({})
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RoleAuthorizationInterceptor {

    /**
     * Service for verifying Keycloak roles. This service checks if the user
     * has the required roles.
     */
    private final KeycloakRoleVerificationService roleVerificationService;

    /**
     * Constructs a new RoleAuthorizationInterceptor.
     *
     * @param roleVerificationService Service for verifying Keycloak roles.
     */
    @Inject
    public RoleAuthorizationInterceptor(
                    KeycloakRoleVerificationService roleVerificationService) {
            this.roleVerificationService = roleVerificationService;
    }

    /**
     * Intercepts method invocations to check for required roles. If the
     * user lacks the necessary roles, a {@link ForbiddenException} is
     * thrown.
     *
     * @param context The invocation context.
     * @return The result of the method invocation if authorization is
     *         successful.
     * @throws Exception if the method invocation fails or access is denied.
     */
    @AroundInvoke
    public Object checkRoles(InvocationContext context) throws Exception {
            RequiresRole annotation = context.getMethod()
                            .getAnnotation(RequiresRole.class);

            if (annotation == null) {
                    annotation = context.getTarget().getClass()
                                    .getAnnotation(RequiresRole.class);
            }

            if (annotation != null) {
                    Roles[] requiredRoles = annotation.value();

                    if (requiredRoles.length > 0 && !roleVerificationService
                                    .hasAnyRole(requiredRoles)) {
                            String roleNames = Arrays.stream(requiredRoles)
                                            .map(Roles::getRoleName)
                                            .collect(Collectors
                                                            .joining(", "));

                            Log.warnf("Access denied: User lacks required role(s): %s",
                                            roleNames);
                            throw new ForbiddenException(
                                            "User lacks required role(s): "
                                                            + roleNames);
                    }
            }

            return context.proceed();
    }
}
