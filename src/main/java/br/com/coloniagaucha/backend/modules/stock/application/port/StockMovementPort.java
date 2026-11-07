package br.com.coloniagaucha.backend.modules.stock.application.port;

import br.com.coloniagaucha.backend.modules.stock.domain.MovementType;
import br.com.coloniagaucha.backend.modules.stock.domain.StockMovement;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface StockMovementPort {
    StockMovement save(StockMovement movement);
    BigDecimal balance(UUID productId);
    PageResult<StockMovement> search(MovementFilter filter, PageQuery page);
    PageResult<BalanceQuantity> balances(PageQuery page);
    boolean existsForProduct(UUID productId);

    record MovementFilter(UUID productId, MovementType type, UUID supplierId, Instant from, Instant to) { }
    record BalanceQuantity(UUID productId, BigDecimal quantity) { }
}
