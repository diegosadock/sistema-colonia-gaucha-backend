package br.com.coloniagaucha.backend.modules.stock.application;

import br.com.coloniagaucha.backend.modules.product.application.port.ProductCatalogPort;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductCatalogPort.ProductReference;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort.BalanceQuantity;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort.MovementFilter;
import br.com.coloniagaucha.backend.modules.stock.domain.MovementType;
import br.com.coloniagaucha.backend.modules.stock.domain.StockMovement;
import br.com.coloniagaucha.backend.modules.supplier.application.port.SupplierCatalogPort;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import br.com.coloniagaucha.backend.shared.domain.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockUseCases {
    private static final java.util.Set<String> SORT_FIELDS = java.util.Set.of("createdAt", "type", "quantity");
    private final StockMovementPort movements;
    private final ProductCatalogPort products;
    private final SupplierCatalogPort suppliers;
    private final Clock clock;

    public StockUseCases(StockMovementPort movements, ProductCatalogPort products,
                         SupplierCatalogPort suppliers, Clock clock) {
        this.movements = movements; this.products = products; this.suppliers = suppliers; this.clock = clock;
    }

    @Transactional
    public StockMovement create(CreateMovement command) {
        ProductReference product = products.lockActiveProduct(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto ativo", command.productId()));
        if (command.supplierId() != null && !suppliers.isActive(command.supplierId())) {
            throw new ResourceNotFoundException("Fornecedor ativo", command.supplierId());
        }
        StockMovement movement = StockMovement.create(command.productId(), command.supplierId(), command.type(),
                command.quantity(), command.reason(), product.unitCode(), clock.instant(), command.createdBy());
        BigDecimal finalBalance = movements.balance(product.id()).add(movement.delta());
        if (finalBalance.signum() < 0) throw new BusinessRuleException("Saldo insuficiente para essa movimentação.");
        return movements.save(movement);
    }

    @Transactional(readOnly = true)
    public PageResult<StockMovement> list(MovementFilter filter, PageQuery page) {
        if (!SORT_FIELDS.contains(page.sort())) throw new BusinessRuleException("Campo de ordenação inválido.");
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BusinessRuleException("from deve ser anterior ou igual a to.");
        }
        return movements.search(filter, page);
    }

    @Transactional(readOnly = true)
    public Balance balance(UUID productId) {
        Map<UUID, String> units = products.unitCodes(List.of(productId));
        String unit = units.get(productId);
        if (unit == null) throw new ResourceNotFoundException("Produto", productId);
        return new Balance(productId, unit, movements.balance(productId));
    }

    @Transactional(readOnly = true)
    public PageResult<Balance> balances(PageQuery page) {
        PageResult<BalanceQuantity> amounts = movements.balances(page);
        Map<UUID, String> units = products.unitCodes(amounts.items().stream().map(BalanceQuantity::productId).toList());
        List<Balance> items = amounts.items().stream()
                .filter(amount -> units.containsKey(amount.productId()))
                .map(amount -> new Balance(amount.productId(), units.get(amount.productId()), amount.quantity()))
                .toList();
        return new PageResult<>(items, amounts.total(), amounts.page(), amounts.size(), amounts.totalPages());
    }

    public record CreateMovement(UUID productId, UUID supplierId, MovementType type,
                                 BigDecimal quantity, String reason, UUID createdBy) { }
    public record Balance(UUID productId, String unitOfMeasure, BigDecimal quantity) { }
}
