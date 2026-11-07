package br.com.coloniagaucha.backend.modules.product.adapter.in.web;

import br.com.coloniagaucha.backend.modules.product.application.ProductUseCases;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductPort.ProductFilter;
import br.com.coloniagaucha.backend.modules.product.domain.Product;
import br.com.coloniagaucha.backend.modules.product.domain.UnitOfMeasure;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductUseCases products;

    public ProductController(ProductUseCases products) { this.products = products; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return ProductResponse.from(products.create(new ProductUseCases.CreateProduct(
                request.name(), request.description(), request.category(),
                UnitOfMeasure.fromCode(request.unitOfMeasure()), request.price())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    PageResult<ProductResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") PageQuery.Direction direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String unitOfMeasure) {
        PageResult<Product> result = products.list(
                new ProductFilter(name, category, active, unitOfMeasure),
                new PageQuery(page, size, sort, direction));
        return new PageResult<>(result.items().stream().map(ProductResponse::from).toList(),
                result.total(), result.page(), result.size(), result.totalPages());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    ProductResponse get(@PathVariable UUID id) { return ProductResponse.from(products.get(id)); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.from(products.update(id, new ProductUseCases.UpdateProduct(
                request.name(), request.description(), request.category(),
                request.unitOfMeasure() == null ? null : UnitOfMeasure.fromCode(request.unitOfMeasure()),
                request.price())));
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deactivate(@PathVariable UUID id) { products.deactivate(id); }

    @PatchMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void reactivate(@PathVariable UUID id) { products.reactivate(id); }

    public record CreateProductRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 255) String description,
            @Size(max = 80) String category,
            @NotBlank @Pattern(regexp = "g|ml|un") String unitOfMeasure,
            @jakarta.validation.constraints.NotNull @DecimalMin(value = "0.00", inclusive = false)
            @Digits(integer = 10, fraction = 2) BigDecimal price) { }

    public record UpdateProductRequest(
            @Size(max = 120) String name,
            @Size(max = 255) String description,
            @Size(max = 80) String category,
            @Pattern(regexp = "g|ml|un") String unitOfMeasure,
            @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price) { }

    public record ProductResponse(
            UUID id,
            String name,
            String description,
            String category,
            String unitOfMeasure,
            BigDecimal price,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.id(), product.name(), product.description(), product.category(),
                    product.unitOfMeasure().code(), product.price(), product.active(), product.createdAt(), product.updatedAt());
        }
    }
}
