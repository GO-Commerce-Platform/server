package dev.tiodati.saas.gocommerce.tenant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that cleans up the TenantContext after each request
 * to prevent memory leaks from ThreadLocal variables.
 */
@Provider
@ApplicationScoped
public class RequestTenantFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Clean up tenant context after the response is sent
        TenantContext.clear();
    }
}