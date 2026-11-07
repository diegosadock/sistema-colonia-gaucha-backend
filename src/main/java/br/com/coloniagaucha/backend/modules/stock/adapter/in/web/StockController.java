package br.com.coloniagaucha.backend.modules.stock.adapter.in.web;

import br.com.coloniagaucha.backend.modules.stock.application.StockUseCases;
import br.com.coloniagaucha.backend.modules.stock.application.port.StockMovementPort.MovementFilter;
import br.com.coloniagaucha.backend.modules.stock.domain.MovementType;
import br.com.coloniagaucha.backend.modules.stock.domain.StockMovement;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/stock")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class StockController {
    private final StockUseCases stock;
    public StockController(StockUseCases stock) { this.stock = stock; }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    StockMovementResponse create(@Valid @RequestBody CreateMovementRequest request, @AuthenticationPrincipal Jwt jwt) {
        StockMovement movement = stock.create(new StockUseCases.CreateMovement(
                request.productId(), request.supplierId(), request.type(), request.quantity(), request.reason(),
                UUID.fromString(jwt.getSubject())));
        return StockMovementResponse.from(movement);
    }

    @GetMapping("/movements")
    PageResult<StockMovementResponse> movements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") PageQuery.Direction direction,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        var result = stock.list(new MovementFilter(productId, movementType, supplierId, from, to),
                new PageQuery(page, size, sort, direction));
        return new PageResult<>(result.items().stream().map(StockMovementResponse::from).toList(),
                result.total(), result.page(), result.size(), result.totalPages());
    }

    @GetMapping(value = "/balances", params = "productId")
    StockUseCases.Balance balance(@RequestParam UUID productId) { return stock.balance(productId); }

    @GetMapping(value = "/balances", params = "!productId")
    PageResult<StockUseCases.Balance> balances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return stock.balances(new PageQuery(page, size, "productId", PageQuery.Direction.ASC));
    }

    public record CreateMovementRequest(
            @NotNull UUID productId,
            UUID supplierId,
            @NotNull MovementType type,
            @NotNull @Digits(integer = 15, fraction = 3) BigDecimal quantity,
            @Size(max = 255) String reason) { }

    public record StockMovementResponse(UUID id, UUID productId, UUID supplierId, MovementType type,
                                        BigDecimal quantity, String reason, String unitOfMeasure,
                                        Instant createdAt, UUID createdBy) {
        static StockMovementResponse from(StockMovement movement) {
            return new StockMovementResponse(movement.id(), movement.productId(), movement.supplierId(), movement.type(),
                    movement.quantity(), movement.reason(), movement.unitCode(), movement.createdAt(), movement.createdBy());
        }
    }
}
