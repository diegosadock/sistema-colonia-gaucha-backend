package br.com.coloniagaucha.backend.modules.stock.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.product.application.port.ProductStockUsagePort;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort.BalanceQuantity;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort.MovementFilter;
import br.com.coloniagaucha.backend.modules.stock.domain.StockMovement;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class StockPersistenceAdapter implements StockMovementPort, ProductStockUsagePort {
    private final StockMovementJpaRepository repository;
    StockPersistenceAdapter(StockMovementJpaRepository repository) { this.repository = repository; }

    @Override @Transactional
    public StockMovement save(StockMovement movement) { return repository.save(new StockMovementJpaEntity(movement)).toDomain(); }

    @Override @Transactional(readOnly = true)
    public java.math.BigDecimal balance(UUID productId) { return repository.balance(productId); }

    @Override @Transactional(readOnly = true)
    public PageResult<StockMovement> search(MovementFilter filter, PageQuery page) {
        Specification<StockMovementJpaEntity> spec = (root, query, builder) -> builder.conjunction();
        if (filter.productId() != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("productId"), filter.productId()));
        if (filter.type() != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("type"), filter.type()));
        if (filter.supplierId() != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("supplierId"), filter.supplierId()));
        if (filter.from() != null) spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
        if (filter.to() != null) spec = spec.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
        Sort sort = Sort.by(page.direction() == PageQuery.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC, page.sort())
                .and(Sort.by(Sort.Direction.ASC, "id"));
        var result = repository.findAll(spec, PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(result.getContent().stream().map(StockMovementJpaEntity::toDomain).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages());
    }

    @Override @Transactional(readOnly = true)
    public PageResult<BalanceQuantity> balances(PageQuery page) {
        var result = repository.balances(PageRequest.of(page.page(), page.size()));
        return new PageResult<>(result.getContent().stream().map(row -> new BalanceQuantity(row.getProductId(), row.getQuantity())).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages());
    }

    @Override @Transactional(readOnly = true)
    public boolean existsForProduct(UUID productId) { return repository.existsByProductId(productId); }

    @Override @Transactional(readOnly = true)
    public boolean hasMovements(UUID productId) { return repository.existsByProductId(productId); }
}
