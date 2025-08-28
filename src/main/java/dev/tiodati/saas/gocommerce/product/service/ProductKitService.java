package dev.tiodati.saas.gocommerce.product.service;

import dev.tiodati.saas.gocommerce.product.dto.CreateProductKitDto;
import dev.tiodati.saas.gocommerce.product.dto.ProductKitDto;
import dev.tiodati.saas.gocommerce.product.dto.UpdateProductKitDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductKitService {

    List<ProductKitDto> findByStoreId(UUID storeId);

    Optional<ProductKitDto> findById(UUID id);

    ProductKitDto create(UUID storeId, CreateProductKitDto createProductKitDto);

    ProductKitDto update(UUID id, UpdateProductKitDto updateProductKitDto);

    void delete(UUID id);
}
