package br.com.coloniagaucha.backend.modules.supplier.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.supplier.application.port.SupplierCatalogPort;
import br.com.coloniagaucha.backend.modules.supplier.application.port.SupplierPort;
import br.com.coloniagaucha.backend.modules.supplier.domain.Supplier;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
class SupplierPersistenceAdapter implements SupplierPort, SupplierCatalogPort {
    private final SupplierJpaRepository repository;
    SupplierPersistenceAdapter(SupplierJpaRepository repository) { this.repository = repository; }

    @Override @Transactional(readOnly = true)
    public java.util.Optional<Supplier> findById(UUID id) { return repository.findById(id).map(SupplierJpaEntity::toDomain); }

    @Override @Transactional
    public java.util.Optional<Supplier> findByIdForUpdate(UUID id) { return repository.findByIdForUpdate(id).map(SupplierJpaEntity::toDomain); }

    @Override @Transactional
    public Supplier save(Supplier supplier) { return repository.save(new SupplierJpaEntity(supplier)).toDomain(); }

    @Override @Transactional(readOnly = true)
    public PageResult<Supplier> search(String name, Boolean active, PageQuery page) {
        Specification<SupplierJpaEntity> spec = (root, query, builder) -> builder.conjunction();
        if (StringUtils.hasText(name)) {
            String pattern = "%" + escapeLike(name.trim().toLowerCase(java.util.Locale.ROOT)) + "%";
            spec = spec.and((root, query, builder) -> builder.like(builder.lower(root.get("name")), pattern, '\\'));
        }
        if (active != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("active"), active));
        Sort sort = Sort.by(page.direction() == PageQuery.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, page.sort())
                .and(Sort.by(Sort.Direction.ASC, "id"));
        var result = repository.findAll(spec, PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(result.getContent().stream().map(SupplierJpaEntity::toDomain).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages());
    }

    @Override @Transactional(readOnly = true)
    public boolean isActive(UUID supplierId) { return repository.findFirstByIdAndActiveTrue(supplierId).isPresent(); }

    private static String escapeLike(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
}
