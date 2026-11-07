package br.com.coloniagaucha.backend.modules.product.application;

import br.com.coloniagaucha.backend.modules.product.application.port.ProductPort;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductPort.ProductFilter;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductStockUsagePort;
import br.com.coloniagaucha.backend.modules.product.domain.Product;
import br.com.coloniagaucha.backend.modules.product.domain.ProductNameConflictException;
import br.com.coloniagaucha.backend.modules.product.domain.UnitOfMeasure;
import br.com.coloniagaucha.backend.shared.application.PageQuery;
import br.com.coloniagaucha.backend.shared.application.PageResult;
import br.com.coloniagaucha.backend.shared.domain.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductUseCases {
    private final ProductPort products;
    private final ProductStockUsagePort stockUsage;
    private final Clock clock;

    public ProductUseCases(ProductPort products, ProductStockUsagePort stockUsage, Clock clock) {
        this.products = products;
        this.stockUsage = stockUsage;
        this.clock = clock;
    }

    @Transactional
    public Product create(CreateProduct command) {
        Product product = Product.create(command.name(), command.description(), command.category(),
                command.unitOfMeasure(), command.price(), clock.instant());
        if (products.findByNameKey(product.nameKey()).isPresent()) throw new ProductNameConflictException();
        return products.save(product);
    }

    @Transactional(readOnly = true)
    public Product get(UUID id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produto", id));
    }

    @Transactional(readOnly = true)
    public PageResult<Product> list(ProductFilter filter, PageQuery page) {
        if (!java.util.Set.of("createdAt", "name", "price", "updatedAt").contains(page.sort())) {
            throw new br.com.coloniagaucha.backend.shared.domain.BusinessRuleException("Campo de ordenação inválido.");
        }
        return products.search(filter, page);
    }

    @Transactional
    public Product update(UUID id, UpdateProduct command) {
        Product product = products.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        var now = clock.instant();
        if (command.name() == null && command.description() == null && command.category() == null
                && command.unitOfMeasure() == null && command.price() == null) {
            throw new br.com.coloniagaucha.backend.shared.domain.BusinessRuleException("Informe ao menos um campo para atualizar.");
        }
        if (command.name() != null && !Product.nameKey(command.name()).equals(product.nameKey())
                && products.findByNameKey(Product.nameKey(command.name())).isPresent()) {
            throw new ProductNameConflictException();
        }
        if (command.name() != null) product.rename(command.name(), now);
        if (command.description() != null) product.changeDescription(command.description(), now);
        if (command.category() != null) product.changeCategory(command.category(), now);
        if (command.unitOfMeasure() != null && command.unitOfMeasure() != product.unitOfMeasure()) {
            if (stockUsage.hasMovements(product.id())) {
                throw new br.com.coloniagaucha.backend.shared.domain.BusinessRuleException("A unidade não pode mudar após o primeiro movimento de estoque.");
            }
            product.changeUnitOfMeasure(command.unitOfMeasure(), now);
        }
        if (command.price() != null) product.changePrice(command.price(), now);
        return products.save(product);
    }

    @Transactional
    public void deactivate(UUID id) { changeActive(id, false); }

    @Transactional
    public void reactivate(UUID id) { changeActive(id, true); }

    private void changeActive(UUID id, boolean active) {
        Product product = products.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        if (active) product.reactivate(clock.instant()); else product.deactivate(clock.instant());
        products.save(product);
    }

    public record CreateProduct(String name, String description, String category, UnitOfMeasure unitOfMeasure, BigDecimal price) { }
    public record UpdateProduct(String name, String description, String category, UnitOfMeasure unitOfMeasure, BigDecimal price) { }
}
