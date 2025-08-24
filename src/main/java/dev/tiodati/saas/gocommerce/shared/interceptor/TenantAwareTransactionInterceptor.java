package dev.tiodati.saas.gocommerce.shared.interceptor;

import java.util.UUID;

import dev.tiodati.saas.gocommerce.store.StoreContext;
import dev.tiodati.saas.gocommerce.store.service.StoreSchemaService;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@TenantAware
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 1)
public class TenantAwareTransactionInterceptor {

    @Inject
    private StoreSchemaService storeSchemaService;

    @AroundInvoke
    public Object manageTenantTransaction(InvocationContext context) throws Exception {
        // Assumption: The first parameter of the intercepted method is the storeId.
        if (context.getParameters().length == 0 || !(context.getParameters()[0] instanceof UUID)) {
            throw new IllegalStateException(
                    "Method " + context.getMethod().getName() + " is annotated with @TenantAware "
                            + "but its first parameter is not a UUID storeId.");
        }
        UUID storeId = (UUID) context.getParameters()[0];

        try {
            return QuarkusTransaction.requiringNew().call(() -> {
                try {
                    // Set the tenant context inside the transaction
                    storeSchemaService.setStoreSchema(storeId);
                    Log.debugf("Interceptor: Set tenant context for store %s inside transaction", storeId);
                    return context.proceed();
                } catch (Exception e) {
                    throw new RuntimeException("Exception during tenant-aware transaction", e);
                }
            });
        } finally {
            StoreContext.clear();
            Log.debugf("Interceptor: Cleared tenant context for store %s", storeId);
        }
    }
}
