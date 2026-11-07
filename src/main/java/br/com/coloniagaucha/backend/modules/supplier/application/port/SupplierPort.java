package br.com.coloniagaucha.backend.modules.supplier.application.port;

import br.com.coloniagaucha.backend.modules.supplier.domain.Supplier;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface SupplierPort {
    Optional<Supplier> findById(UUID id);
    Optional<Supplier> findByIdForUpdate(UUID id);
    Supplier save(Supplier supplier);
    PageResult<Supplier> search(String name, Boolean active, PageQuery page);
}
