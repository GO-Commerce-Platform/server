package dev.tiodati.saas.gocommerce.product.repository;

import dev.tiodati.saas.gocommerce.product.entity.ProductKit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProductKitRepository implements PanacheRepository<ProductKit> {

    public List<ProductKit> findByStoreId(UUID storeId) {
        return list("storeId", storeId);
    }
}
