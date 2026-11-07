package br.com.coloniagaucha.backend.modules.product.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.product.application.port.ProductCatalogPort;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductPort;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductPort.ProductFilter;
import br.com.coloniagaucha.backend.modules.product.domain.Product;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
class ProductPersistenceAdapter implements ProductPort, ProductCatalogPort {
    private final ProductJpaRepository repository;

    ProductPersistenceAdapter(ProductJpaRepository repository) { this.repository = repository; }

    @Override @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) { return repository.findById(id).map(ProductJpaEntity::toDomain); }

    @Override @Transactional
    public Optional<Product> findByIdForUpdate(UUID id) { return repository.findByIdForUpdate(id).map(ProductJpaEntity::toDomain); }

    @Override @Transactional(readOnly = true)
    public Optional<Product> findByNameKey(String key) { return repository.findByNameKey(key).map(ProductJpaEntity::toDomain); }

    @Override @Transactional
    public Product save(Product product) { return repository.save(new ProductJpaEntity(product)).toDomain(); }

    @Override @Transactional(readOnly = true)
    public PageResult<Product> search(ProductFilter filter, PageQuery page) {
        Specification<ProductJpaEntity> spec = (root, query, builder) -> builder.conjunction();
        if (StringUtils.hasText(filter.name())) {
            String needle = escapeLike(Product.nameKey(filter.name()));
            spec = spec.and((root, query, builder) -> builder.like(root.get("nameKey"), "%" + needle + "%", '\\'));
        }
        if (StringUtils.hasText(filter.category())) {
            spec = spec.and((root, query, builder) -> builder.equal(builder.lower(root.get("category")), filter.category().trim().toLowerCase(java.util.Locale.ROOT)));
        }
        if (filter.active() != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("active"), filter.active()));
        }
        if (StringUtils.hasText(filter.unitCode())) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("unitOfMeasure"), br.com.coloniagaucha.backend.modules.product.domain.UnitOfMeasure.fromCode(filter.unitCode())));
        }
        Sort sort = Sort.by(page.direction() == PageQuery.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, page.sort())
                .and(Sort.by(Sort.Direction.ASC, "id"));
        var result = repository.findAll(spec, PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(result.getContent().stream().map(ProductJpaEntity::toDomain).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages());
    }

    @Override @Transactional
    public Optional<ProductReference> lockActiveProduct(UUID id) {
        return repository.findActiveByIdForUpdate(id)
                .map(entity -> new ProductReference(entity.getId(), entity.getUnitOfMeasure().code()));
    }

    @Override @Transactional(readOnly = true)
    public java.util.Map<UUID, String> unitCodes(java.util.Collection<UUID> ids) {
        return repository.findAllByIdIn(ids).stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                ProductJpaEntity::getId, entity -> entity.getUnitOfMeasure().code()));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
