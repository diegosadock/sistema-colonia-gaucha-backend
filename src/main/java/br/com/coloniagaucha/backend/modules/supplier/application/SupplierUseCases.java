package br.com.coloniagaucha.backend.modules.supplier.application;

import br.com.coloniagaucha.backend.modules.supplier.application.port.SupplierPort;
import br.com.coloniagaucha.backend.modules.supplier.domain.Supplier;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import br.com.coloniagaucha.backend.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierUseCases {
    private static final Set<String> SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");
    private final SupplierPort suppliers;
    private final Clock clock;

    public SupplierUseCases(SupplierPort suppliers, Clock clock) { this.suppliers = suppliers; this.clock = clock; }

    @Transactional
    public Supplier create(String name, String email, String phone) {
        return suppliers.save(Supplier.create(name, email, phone, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Supplier get(UUID id) { return suppliers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id)); }

    @Transactional(readOnly = true)
    public PageResult<Supplier> list(String name, Boolean active, PageQuery page) {
        if (!SORT_FIELDS.contains(page.sort())) throw new BusinessRuleException("Campo de ordenação inválido.");
        return suppliers.search(name, active, page);
    }

    @Transactional
    public Supplier update(UUID id, UpdateSupplier command) {
        Supplier supplier = suppliers.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
        if (command.name() == null && command.email() == null && command.phone() == null) {
            throw new BusinessRuleException("Informe ao menos um campo para atualizar.");
        }
        supplier.update(command.name(), command.email(), command.phone(), clock.instant());
        return suppliers.save(supplier);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        Supplier supplier = suppliers.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
        supplier.setActive(active, clock.instant());
        suppliers.save(supplier);
    }

    public record UpdateSupplier(String name, String email, String phone) { }
}
