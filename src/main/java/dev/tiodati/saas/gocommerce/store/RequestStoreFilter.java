package dev.tiodati.saas.gocommerce.store;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that cleans up the StoreContext after each reques
 * to prevent memory leaks from ThreadLocal variables.
 */
@Provider
@ApplicationScoped
public class RequestStoreFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Clean up store context after the response is sen
        StoreContext.clear();
    }
}
