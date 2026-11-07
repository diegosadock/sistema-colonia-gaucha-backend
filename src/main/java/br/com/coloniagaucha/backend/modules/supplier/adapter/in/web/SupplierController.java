package br.com.coloniagaucha.backend.modules.supplier.adapter.in.web;

import br.com.coloniagaucha.backend.modules.supplier.application.SupplierUseCases;
import br.com.coloniagaucha.backend.modules.supplier.domain.Supplier;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
    private final SupplierUseCases suppliers;
    public SupplierController(SupplierUseCases suppliers) { this.suppliers = suppliers; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    SupplierResponse create(@Valid @RequestBody CreateSupplierRequest request) {
        return SupplierResponse.from(suppliers.create(request.name(), request.email(), request.phone()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    PageResult<SupplierResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") PageQuery.Direction direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active) {
        var result = suppliers.list(name, active, new PageQuery(page, size, sort, direction));
        return new PageResult<>(result.items().stream().map(SupplierResponse::from).toList(), result.total(), result.page(), result.size(), result.totalPages());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    SupplierResponse get(@PathVariable UUID id) { return SupplierResponse.from(suppliers.get(id)); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSupplierRequest request) {
        return SupplierResponse.from(suppliers.update(id, new SupplierUseCases.UpdateSupplier(request.name(), request.email(), request.phone())));
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void deactivate(@PathVariable UUID id) { suppliers.setActive(id, false); }

    @PatchMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void reactivate(@PathVariable UUID id) { suppliers.setActive(id, true); }

    public record CreateSupplierRequest(
            @NotBlank @Size(max = 150) String name,
            @Email @Size(max = 254) String email,
            @Pattern(regexp = "[+0-9() .-]{1,32}") String phone) { }

    public record UpdateSupplierRequest(
            @Size(max = 150) String name,
            @Email @Size(max = 254) String email,
            @Pattern(regexp = "[+0-9() .-]{1,32}") String phone) { }

    public record SupplierResponse(UUID id, String name, String email, String phone,
                                   boolean active, Instant createdAt, Instant updatedAt) {
        static SupplierResponse from(Supplier supplier) {
            return new SupplierResponse(supplier.id(), supplier.name(), supplier.email(), supplier.phone(),
                    supplier.active(), supplier.createdAt(), supplier.updatedAt());
        }
    }
}
