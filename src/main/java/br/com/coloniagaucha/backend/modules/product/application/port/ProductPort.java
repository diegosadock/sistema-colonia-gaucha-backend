package br.com.coloniagaucha.backend.modules.product.application.port;

import br.com.coloniagaucha.backend.modules.product.domain.Product;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface ProductPort {
    Optional<Product> findById(UUID id);
    Optional<Product> findByIdForUpdate(UUID id);
    Optional<Product> findByNameKey(String nameKey);
    Product save(Product product);
    PageResult<Product> search(ProductFilter filter, PageQuery page);

    record ProductFilter(String name, String category, Boolean active, String unitCode) { }
}
