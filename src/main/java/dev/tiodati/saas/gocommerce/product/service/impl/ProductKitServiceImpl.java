package dev.tiodati.saas.gocommerce.product.service.impl;

import dev.tiodati.saas.gocommerce.product.dto.*;
import dev.tiodati.saas.gocommerce.product.entity.KitItem;
import dev.tiodati.saas.gocommerce.product.entity.Product;
import dev.tiodati.saas.gocommerce.product.entity.ProductKit;
import dev.tiodati.saas.gocommerce.product.repository.ProductKitRepository;
import dev.tiodati.saas.gocommerce.product.service.ProductKitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import io.quarkus.logging.Log;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductKitServiceImpl implements ProductKitService {

    private final ProductKitRepository productKitRepository;
    private final EntityManager entityManager;

    @Inject
    public ProductKitServiceImpl(ProductKitRepository productKitRepository, EntityManager entityManager) {
        this.productKitRepository = productKitRepository;
        this.entityManager = entityManager;
    }

    @Override
    public List<ProductKitDto> findByStoreId(UUID storeId) {
        return productKitRepository.findByStoreId(storeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductKitDto> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(ProductKit.class, id))
                .map(this::toDto);
    }

    @Override
    @Transactional
    public ProductKitDto create(UUID storeId, CreateProductKitDto createProductKitDto) {
        ProductKit productKit = toEntity(storeId, createProductKitDto);
        entityManager.persist(productKit);
        return toDto(productKit);
    }

    @Override
    @Transactional
    public ProductKitDto update(UUID id, UpdateProductKitDto updateProductKitDto) {
        ProductKit productKit = entityManager.find(ProductKit.class, id);
        if (productKit == null) {
            Log.warnf("ProductKit not found for ID: %s", id);
            throw new EntityNotFoundException("ProductKit not found with id: " + id);
        }

        Log.infof("Updating ProductKit: %s", id);
        productKit.setName(updateProductKitDto.name());
        productKit.setDescription(updateProductKitDto.description());
        productKit.setPrice(updateProductKitDto.price());
        productKit.setActive(updateProductKitDto.isActive());

        // Update items
        productKit.getItems().clear();
        List<KitItem> items = updateProductKitDto.items().stream()
            .map(itemDto -> toEntity(itemDto, productKit))
            .collect(Collectors.toList());
        productKit.getItems().addAll(items);

        Log.infof("ProductKit updated successfully: %s", id);
        return toDto(productKit);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ProductKit productKit = entityManager.find(ProductKit.class, id);
        if (productKit == null) {
            Log.warnf("ProductKit not found for deletion: %s", id);
            throw new EntityNotFoundException("ProductKit not found with id: " + id);
        }
        
        Log.infof("Deleting ProductKit: %s", id);
        entityManager.remove(productKit);
    }

    private ProductKitDto toDto(ProductKit productKit) {
        return new ProductKitDto(
            productKit.getId(),
            productKit.getStoreId(),
            productKit.getName(),
            productKit.getDescription(),
            productKit.getPrice(),
            productKit.isActive(),
            productKit.getItems().stream().map(this::toDto).collect(Collectors.toList()),
            productKit.getCreatedAt(),
            productKit.getUpdatedAt()
        );
    }

    private KitItemDto toDto(KitItem kitItem) {
        return new KitItemDto(
            kitItem.getId(),
            kitItem.getProduct().getId(),
            kitItem.getQuantity()
        );
    }

    private ProductKit toEntity(UUID storeId, CreateProductKitDto createProductKitDto) {
        ProductKit productKit = ProductKit.builder()
            .storeId(storeId)
            .name(createProductKitDto.name())
            .description(createProductKitDto.description())
            .price(createProductKitDto.price())
            .isActive(createProductKitDto.isActive())
            .build();

        List<KitItem> items = createProductKitDto.items().stream()
            .map(itemDto -> toEntity(itemDto, productKit))
            .collect(Collectors.toList());

        productKit.setItems(items);

        return productKit;
    }

    private KitItem toEntity(CreateKitItemDto createKitItemDto, ProductKit productKit) {
        return KitItem.builder()
            .product(entityManager.find(Product.class, createKitItemDto.productId()))
            .quantity(createKitItemDto.quantity())
            .kit(productKit)
            .build();
    }

    private KitItem toEntity(UpdateKitItemDto updateKitItemDto, ProductKit productKit) {
        return KitItem.builder()
            .id(updateKitItemDto.id())
            .product(entityManager.find(Product.class, updateKitItemDto.productId()))
            .quantity(updateKitItemDto.quantity())
            .kit(productKit)
            .build();
    }
}
